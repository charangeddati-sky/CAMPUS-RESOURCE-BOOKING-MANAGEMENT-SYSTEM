package com.college.booking.service;

import com.college.booking.dto.AiDtos.BookByLanguageResponse;
import com.college.booking.dto.AiDtos.ChatResponse;
import com.college.booking.dto.AiDtos.ChatTurn;
import com.college.booking.dto.AiDtos.Intent;
import com.college.booking.dto.AiDtos.InterpretResponse;
import com.college.booking.dto.AiDtos.Recommendation;
import com.college.booking.dto.BookingDtos.CreateBookingRequest;
import com.college.booking.dto.CampusDtos.ResourceCard;
import com.college.booking.entity.Building;
import com.college.booking.entity.Resource;
import com.college.booking.entity.User;
import com.college.booking.exception.ApiException;
import com.college.booking.repository.BookingRepository;
import com.college.booking.repository.BuildingRepository;
import com.college.booking.repository.ResourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AiService {

    private static final Set<String> TYPES = Set.of(
            "CLASSROOM", "LABORATORY", "SEMINAR_HALL", "LIBRARY",
            "AUDITORIUM", "EXAMINATION_HALL", "SPORTS_FACILITY", "EQUIPMENT"
    );

    private final GroqClient groqClient;
    private final CampusService campusService;
    private final AvailabilityService availabilityService;
    private final ResourceRepository resourceRepository;
    private final BuildingRepository buildingRepository;
    private final BookingRepository bookingRepository;
    private final AnalyticsService analyticsService;
    private final BookingService bookingService;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public AiService(GroqClient groqClient, CampusService campusService, AvailabilityService availabilityService,
                     ResourceRepository resourceRepository, BuildingRepository buildingRepository,
                     BookingRepository bookingRepository, AnalyticsService analyticsService,
                     BookingService bookingService, Clock clock, ObjectMapper objectMapper) {
        this.groqClient = groqClient;
        this.campusService = campusService;
        this.availabilityService = availabilityService;
        this.resourceRepository = resourceRepository;
        this.buildingRepository = buildingRepository;
        this.bookingRepository = bookingRepository;
        this.analyticsService = analyticsService;
        this.bookingService = bookingService;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public InterpretResponse interpret(String prompt, User user) {
        Intent intent = extractIntent(prompt);
        intent = sanitize(intent);
        List<Recommendation> recs = recommend(intent, user);
        String explanation = explain(intent, recs);
        return new InterpretResponse(intent, recs, explanation, groqClient.isConfigured());
    }

    public BookByLanguageResponse bookFromLanguage(String prompt, User user, Boolean confirm) {
        if (prompt == null || prompt.isBlank()) {
            return clarify(null, List.of(), "Tell me the room, date, and time you need.",
                    List.of("Which room?", "Which date?", "What start and end time?"));
        }
        Intent intent = sanitize(extractIntent(prompt));
        List<Recommendation> recs = recommend(intent, user);
        List<String> missing = missingSlots(intent);
        boolean wantsBook = wantsBooking(prompt, intent);
        List<Resource> exact = resolveExact(intent);

        if (!missing.isEmpty()) {
            return clarify(intent, recs,
                    "I need a bit more information before I can book.",
                    missing);
        }
        if (exact.size() > 1) {
            return new BookByLanguageResponse("AMBIGUOUS",
                    "Several rooms match that description. Pick one to continue.",
                    List.of("Which of these rooms did you mean?"),
                    intent, recs, null, groqClient.isConfigured());
        }
        if (exact.isEmpty() && recs.stream().filter(Recommendation::available).count() != 1 && recs.size() != 1) {
            if (recs.isEmpty()) {
                return new BookByLanguageResponse("UNAVAILABLE",
                        "No matching resource exists in the campus database for that request.",
                        List.of("Try a room name, code, or browse the campus map."),
                        intent, recs, null, groqClient.isConfigured());
            }
            return new BookByLanguageResponse("AMBIGUOUS",
                    "I found more than one match. Choose a room or be more specific.",
                    List.of("Which resource should I book?"),
                    intent, recs, null, groqClient.isConfigured());
        }

        Resource target = exact.size() == 1
                ? exact.get(0)
                : resourceRepository.findById(recs.get(0).resourceId()).orElse(null);
        if (target == null) {
            return new BookByLanguageResponse("UNAVAILABLE",
                    "That resource does not exist.", List.of(), intent, recs, null, groqClient.isConfigured());
        }
        LocalDate date = parseDate(intent.date());
        LocalTime start = parseTime(intent.startTime());
        LocalTime end = parseTime(intent.endTime());
        String reason = availabilityService.unavailableReason(target, date, start, end);
        if (reason != null) {
            return new BookByLanguageResponse("UNAVAILABLE",
                    reason + " Here are alternatives from SQL.",
                    List.of("Pick another time or room."),
                    intent, recs, null, groqClient.isConfigured());
        }
        boolean confirmedInPrompt = prompt.toLowerCase(Locale.ROOT).contains("confirm")
                || prompt.toLowerCase(Locale.ROOT).contains("yes,");
        if (!Boolean.TRUE.equals(confirm) && !confirmedInPrompt) {
            return new BookByLanguageResponse("CONFIRM",
                    "I can book " + target.getName() + " on " + date + " from " + start + " to " + end
                            + (intent.capacity() != null ? " for " + intent.capacity() + " attendees" : "")
                            + ". Confirm to submit the request through the normal approval workflow.",
                    List.of("Confirm this booking?"),
                    intent, recs, null, groqClient.isConfigured());
        }
        try {
            var view = bookingService.create(user, new CreateBookingRequest(
                    List.of(target.getId()), null, date, start, end,
                    target.getName() + " booking", prompt, intent.capacity(), null, null, null, "RESOURCE"));
            return new BookByLanguageResponse("BOOKED",
                    "Booked " + target.getName() + " on " + date + " from " + start + " to " + end
                            + ". Status: " + view.status() + ". This used the same server-side validation as the booking form.",
                    List.of(), intent, recs, view, groqClient.isConfigured());
        } catch (ApiException ex) {
            return new BookByLanguageResponse("UNAVAILABLE", ex.getMessage(),
                    List.of(), intent, recs, null, groqClient.isConfigured());
        }
    }

    private BookByLanguageResponse clarify(Intent intent, List<Recommendation> recs, String message, List<String> questions) {
        return new BookByLanguageResponse("CLARIFY", message, questions, intent, recs, null, groqClient.isConfigured());
    }

    private boolean wantsBooking(String prompt, Intent intent) {
        String p = (prompt == null ? "" : prompt).toLowerCase(Locale.ROOT);
        String i = intent == null || intent.intent() == null ? "" : intent.intent().toUpperCase(Locale.ROOT);
        return p.contains("book") || p.contains("reserve") || p.contains("schedule") || "BOOK".equals(i);
    }

    private List<String> missingSlots(Intent intent) {
        List<String> missing = new ArrayList<>();
        if (intent.date() == null) {
            missing.add("Which date? (today, tomorrow, or YYYY-MM-DD)");
        }
        if (intent.startTime() == null || intent.endTime() == null) {
            missing.add("What start and end time?");
        }
        if ((intent.resourceName() == null || intent.resourceName().isBlank())
                && (intent.query() == null || intent.query().isBlank())
                && intent.resourceType() == null) {
            missing.add("Which room or resource type?");
        }
        return missing;
    }

    private List<Resource> resolveExact(Intent intent) {
        List<String> hints = new ArrayList<>();
        if (intent.resourceName() != null && !intent.resourceName().isBlank()) {
            hints.add(intent.resourceName().trim());
        }
        if (intent.query() != null && !intent.query().isBlank()) {
            hints.add(intent.query().trim());
        }
        List<Resource> exact = new ArrayList<>();
        for (String hint : hints) {
            resourceRepository.findByCodeIgnoreCase(hint).ifPresent(exact::add);
            resourceRepository.findByNameIgnoreCase(hint).ifPresent(exact::add);
            if (exact.isEmpty()) {
                List<Resource> contains = resourceRepository.findByEnabledTrueAndNameContainingIgnoreCase(hint);
                if (contains.size() == 1) {
                    exact.add(contains.get(0));
                } else if (contains.size() > 1 && exact.isEmpty()) {
                    return contains;
                }
            }
        }
        if (exact.isEmpty()) {
            List<Resource> all = resourceRepository.findAllEnabledDetailed();
            for (String hint : hints) {
                String h = hint.toLowerCase(Locale.ROOT);
                for (Resource r : all) {
                    String rn = r.getName().toLowerCase(Locale.ROOT);
                    if (h.contains(rn) || rn.contains(h)) {
                        exact.add(r);
                    }
                }
            }
        }
        return exact.stream().distinct().toList();
    }

    public ChatResponse chat(String message, List<ChatTurn> history, User user) {
        Intent intent = extractIntent(message);
        intent = sanitize(intent);
        String lower = message.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> data = new ArrayList<>();
        String facts;

        if (lower.contains("upcoming booking") || lower.contains("my booking")) {
            var mine = bookingRepository.findByUserIdOrderByBookingDateDescStartTimeDesc(user.getId());
            facts = mine.isEmpty() ? "The user has no bookings." : mine.stream().limit(5)
                    .map(b -> b.getTitle() + " on " + b.getBookingDate() + " " + b.getStartTime() + "–" + b.getEndTime()
                            + " (" + b.getStatus() + ")")
                    .reduce((a, b) -> a + "; " + b).orElse("");
            mine.stream().limit(5).forEach(b -> data.add(Map.of("bookingId", b.getId(), "title", b.getTitle(), "status", b.getStatus())));
        } else if (lower.contains("available now") || lower.contains("available right now")) {
            List<ResourceCard> now = campusService.availableNow(null, null, null, user);
            facts = now.isEmpty() ? "No resources are available right now." :
                    now.stream().limit(12).map(r -> r.name() + " (" + r.buildingName() + ")").reduce((a, b) -> a + ", " + b).orElse("");
            now.stream().limit(12).forEach(r -> data.add(Map.of("id", r.id(), "name", r.name(), "building", r.buildingName())));
        } else if (lower.contains("where is") || lower.contains("which block") || lower.contains("largest")) {
            String q = searchableQuery(intent.query());
            if (q == null) {
                q = searchableQuery(message.replace("where is", "").replace("which block", "").trim());
            }
            List<Resource> found = resourceRepository.search(q,
                    null, null, intent.resourceType(), null, intent.capacity());
            if (found.isEmpty()) {
                facts = "No matching resource exists in the database.";
            } else {
                facts = found.stream().limit(8)
                        .map(r -> r.getName() + " is in " + r.getBuilding().getName() + ", " + r.getFloor().getName()
                                + " (capacity " + r.getCapacity() + ")")
                        .reduce((a, b) -> a + "; " + b).orElse("");
                found.stream().limit(8).forEach(r -> data.add(Map.of(
                        "id", r.getId(), "name", r.getName(), "building", r.getBuilding().getName(), "floor", r.getFloor().getName())));
            }
        } else {
            List<Recommendation> recs = recommend(intent, user);
            facts = recs.isEmpty()
                    ? "No matching resources exist in SQL for this request."
                    : recs.stream().map(r -> r.resourceName() + " score " + r.score() + " — " + r.reason())
                    .reduce((a, b) -> a + "; " + b).orElse("");
            recs.forEach(r -> data.add(Map.of("id", r.resourceId(), "name", r.resourceName(), "score", r.score(), "reason", r.reason())));
        }

        String reply;
        try {
            if (!groqClient.isConfigured()) {
                throw new IllegalStateException("GROQ_UNAVAILABLE");
            }
            reply = groqClient.chat(
                    "You are the VVIT campus assistant. Answer ONLY using the provided facts. Never invent rooms, buildings, or statistics. If facts are empty, say you could not find a match and suggest using the campus map or manual booking. Keep the tone helpful and concise.",
                    "User question: " + message + "\nFacts from the database:\n" + facts
            );
        } catch (Exception ex) {
            reply = composeFallbackReply(message, facts, recsFromData(data));
            return new ChatResponse(reply, data, false);
        }
        return new ChatResponse(reply, data, true);
    }

    private String composeFallbackReply(String message, String facts, boolean hasRecs) {
        if (facts == null || facts.isBlank()) {
            return "I could not find a match in the campus database. Try the campus map or the booking wizard.";
        }
        return facts;
    }

    private boolean recsFromData(List<Map<String, Object>> data) {
        return data != null && !data.isEmpty();
    }

    public String insights(String question) {
        var overview = analyticsService.overview(null, null);
        String facts = "KPIs=" + overview.kpis()
                + " statusMix=" + overview.statusMix()
                + " mostBooked=" + overview.mostBooked()
                + " predictions(kind=FORECAST, not live)=" + overview.predictions();
        try {
            return groqClient.chat(
                    "You are a campus operations analyst. Explain the provided statistics. Never invent numbers. Clearly distinguish live occupancy from FORECAST predictions. If the question cannot be answered from the facts, say so.",
                    "Question: " + question + "\nStatistics: " + facts
            );
        } catch (Exception ex) {
            return "Here are the live campus statistics from SQL: " + facts;
        }
    }

    private Intent extractIntent(String prompt) {
        if (groqClient.isConfigured()) {
            try {
                String json = groqClient.chatJson(
                        "Extract a booking intent as JSON with keys: resourceType (CLASSROOM|LABORATORY|SEMINAR_HALL|LIBRARY|AUDITORIUM|EXAMINATION_HALL|null), capacity (int or null, only when the user states a group size), date (YYYY-MM-DD or null, today is "
                                + LocalDate.now(clock) + ", tomorrow is " + LocalDate.now(clock).plusDays(1)
                                + "), startTime (HH:mm or null), endTime (HH:mm or null), requiredFacilities (array of codes like PROJECTOR, SMART_BOARD, AC, WIFI, AUDIO, MIC, COMPUTERS), building (string or null), query (short search phrase), resourceName (specific room name or code if given), intent (FIND|LOCATE|AVAILABILITY|BOOKINGS|ANALYTICS|BOOK).",
                        prompt
                );
                return objectMapper.readValue(json, Intent.class);
            } catch (Exception ignored) {
                // fall through to heuristic parser
            }
        }
        return heuristic(prompt);
    }

    private Intent sanitize(Intent intent) {
        String type = intent.resourceType();
        if (type != null) {
            type = type.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            if (type.contains("LAB")) type = "LABORATORY";
            if (type.contains("SEMINAR") || type.contains("HALL") && type.contains("SEM")) type = "SEMINAR_HALL";
            if (type.contains("CLASS")) type = "CLASSROOM";
            if (!TYPES.contains(type)) type = null;
        }
        LocalDate date = parseDate(intent.date());
        LocalTime start = parseTime(intent.startTime());
        LocalTime end = parseTime(intent.endTime());
        if ((start == null || end == null) && intent.query() != null) {
            Intent fromPrompt = heuristic(intent.query());
            LocalTime hs = parseTime(fromPrompt.startTime());
            LocalTime he = parseTime(fromPrompt.endTime());
            if (hs != null && he != null && he.isAfter(hs)) {
                start = hs;
                end = he;
            }
        }
        if (end != null && (end.equals(LocalTime.MIDNIGHT) || (start != null && !end.isAfter(start)))) {
            if (start != null && end.getHour() > 0 && end.getHour() < 12) {
                end = end.plusHours(12);
            }
        }
        Integer cap = intent.capacity() != null && intent.capacity() > 0 ? intent.capacity() : null;
        List<String> facilities = intent.requiredFacilities() == null ? List.of() : intent.requiredFacilities().stream()
                .map(f -> f.trim().toUpperCase(Locale.ROOT).replace(' ', '_'))
                .toList();
        String resourceName = intent.resourceName() == null ? null : intent.resourceName().trim();
        if (resourceName != null && resourceName.isBlank()) {
            resourceName = null;
        }
        return new Intent(type, cap, date == null ? null : date.toString(),
                start == null ? null : start.toString(),
                end == null ? null : end.toString(),
                facilities, intent.building(), intent.query(), intent.intent(), resourceName);
    }

    public List<Recommendation> recommend(Intent intent, User user) {
        Long buildingId = null;
        if (intent.building() != null && !intent.building().isBlank()) {
            buildingId = buildingRepository.findAll().stream()
                    .filter(b -> b.getName().toLowerCase(Locale.ROOT).contains(intent.building().toLowerCase(Locale.ROOT))
                            || b.getCode().equalsIgnoreCase(intent.building())
                            || (b.getVirtueName() != null && b.getVirtueName().toLowerCase(Locale.ROOT).contains(intent.building().toLowerCase(Locale.ROOT))))
                    .map(Building::getId)
                    .findFirst()
                    .orElse(null);
        }
        LocalDate date = parseDate(intent.date());
        LocalTime start = parseTime(intent.startTime());
        LocalTime end = parseTime(intent.endTime());
        String query = searchableQuery(intent.query());
        List<ResourceCard> cards = campusService.search(
                query, buildingId, null, intent.resourceType(), null,
                intent.capacity(), date, start, end, intent.requiredFacilities(), user);

        // If time filters removed everything because they were too strict, still score existing SQL resources.
        if (cards.isEmpty()) {
            cards = campusService.search(query, buildingId, null, intent.resourceType(), null,
                    intent.capacity(), null, null, null, intent.requiredFacilities(), user);
        }

        List<Recommendation> recs = new ArrayList<>();
        for (ResourceCard card : cards) {
            Resource resource = resourceRepository.findById(card.id()).orElseThrow();
            int score = 0;
            StringBuilder why = new StringBuilder();
            boolean available = true;
            if (date != null && start != null && end != null) {
                String reason = availabilityService.unavailableReason(resource, date, start, end);
                if (reason == null) {
                    score += 40;
                    why.append("Available for the requested time. ");
                } else {
                    available = false;
                    why.append(reason).append(" ");
                }
            } else if (availabilityService.isAvailableNow(resource)) {
                score += 25;
                why.append("Currently free. ");
            }
            if (intent.capacity() != null) {
                int cap = resource.getCapacity() == null ? 0 : resource.getCapacity();
                if (cap >= intent.capacity()) {
                    int waste = cap - intent.capacity();
                    int fit = Math.max(0, 25 - waste / 4);
                    score += fit;
                    if (waste <= 10) {
                        why.append("Capacity ").append(cap).append(" closely fits ").append(intent.capacity()).append(". ");
                    } else {
                        why.append("Capacity ").append(cap).append(" can host ").append(intent.capacity()).append(". ");
                    }
                }
            } else {
                score += 5;
            }
            if (intent.resourceType() != null && intent.resourceType().equalsIgnoreCase(card.typeCode())) {
                score += 15;
                why.append("Matches requested type. ");
            }
            if (intent.requiredFacilities() != null) {
                long matched = intent.requiredFacilities().stream()
                        .filter(f -> card.facilities().stream().anyMatch(cf -> cf.equalsIgnoreCase(f)
                                || cf.replace(" ", "_").equalsIgnoreCase(f)))
                        .count();
                score += (int) (matched * 5);
                if (matched > 0) {
                    why.append("Has the requested facilities. ");
                }
            }
            recs.add(new Recommendation(card.id(), card.name(), card.code(), score,
                    why.toString().isBlank() ? "Matches campus inventory." : why.toString().trim(), available));
        }
        recs.sort(Comparator.comparingInt(Recommendation::score).reversed());
        return recs.stream().limit(8).toList();
    }

    private String explain(Intent intent, List<Recommendation> recs) {
        if (recs.isEmpty()) {
            return "No matching resource exists in the campus database. Try a different type, capacity, or time, or book manually from the campus map.";
        }
        Recommendation top = recs.get(0);
        try {
            if (groqClient.isConfigured()) {
                return groqClient.chat(
                        "Write one short recommendation sentence using only the given facts. Do not invent rooms.",
                        "Requested: " + intent + " Best: " + top.resourceName() + " (" + top.resourceCode() + ") — " + top.reason()
                );
            }
        } catch (IllegalStateException ignored) {
        }
        return top.resourceName() + " is recommended. " + top.reason();
    }

    private Intent heuristic(String prompt) {
        String p = prompt.toLowerCase(Locale.ROOT);
        String type = null;
        if (p.contains("seminar")) type = "SEMINAR_HALL";
        else if (p.contains("lab")) type = "LABORATORY";
        else if (p.contains("library")) type = "LIBRARY";
        else if (p.contains("auditorium")) type = "AUDITORIUM";
        else if (p.contains("exam")) type = "EXAMINATION_HALL";
        else if (p.contains("conference")) type = "CLASSROOM";
        else if (p.contains("class") || p.contains("room")) type = "CLASSROOM";

        String resourceName = null;
        Integer cap = null;
        var capM = java.util.regex.Pattern.compile("(?:for|of|with|around|about|upto)\\s*(\\d+)\\s*(?:students|people|seats|attendees|participants|delegates)?").matcher(p);
        var capM2 = java.util.regex.Pattern.compile("(\\d+)\\s*(?:students|people|seats|attendees|participants|delegates)").matcher(p);
        if (capM.find()) {
            cap = Integer.parseInt(capM.group(1));
        } else if (capM2.find()) {
            cap = Integer.parseInt(capM2.group(1));
        }

        LocalDate date = p.contains("tomorrow") ? LocalDate.now(clock).plusDays(1)
                : p.contains("today") ? LocalDate.now(clock)
                : p.contains("yesterday") ? LocalDate.now(clock).minusDays(1) : null;
        if (date == null) {
            var isoDate = java.util.regex.Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b").matcher(prompt);
            if (isoDate.find()) {
                date = parseDate(isoDate.group(1));
            } else {
                var dmy = java.util.regex.Pattern.compile("\\b(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})\\b").matcher(prompt);
                if (dmy.find()) {
                    try {
                        int day = Integer.parseInt(dmy.group(1));
                        int month = Integer.parseInt(dmy.group(2));
                        int year = Integer.parseInt(dmy.group(3));
                        date = LocalDate.of(year, month, day);
                    } catch (Exception ignored) {}
                }
            }
        }

        LocalTime start = null;
        LocalTime end = null;

        // 0. Extract explicit context tags if present
        if (p.contains("[context:") || p.contains("time:")) {
            var ctxTime = java.util.regex.Pattern.compile("time:\\s*(\\d{1,2}:\\d{2})\\s*(?:to|-|–)\\s*(\\d{1,2}:\\d{2})").matcher(p);
            if (ctxTime.find()) {
                start = parseTime(ctxTime.group(1));
                end = parseTime(ctxTime.group(2));
            }
            var ctxDate = java.util.regex.Pattern.compile("date:\\s*(\\d{4}-\\d{2}-\\d{2})").matcher(p);
            if (ctxDate.find()) {
                date = parseDate(ctxDate.group(1));
            }
            var ctxCap = java.util.regex.Pattern.compile("capacity:\\s*(\\d+)").matcher(p);
            if (ctxCap.find()) {
                cap = Integer.parseInt(ctxCap.group(1));
            }
            var ctxRoom = java.util.regex.Pattern.compile("room:\\s*([^\\]\\,]+)").matcher(p);
            if (ctxRoom.find()) {
                resourceName = ctxRoom.group(1).trim();
            }
        }

        // 1. Explicit colon-separated time range: HH:mm to HH:mm (or HH:mm - HH:mm)
        if (start == null || end == null) {
            var timeColon = java.util.regex.Pattern.compile("\\b(\\d{1,2}:\\d{2})\\s*(am|pm)?\\s*(?:to|-|–|until)\\s*(\\d{1,2}:\\d{2})\\s*(am|pm)?\\b").matcher(p);
            if (timeColon.find()) {
                start = parseTime(timeColon.group(1) + (timeColon.group(2) != null ? timeColon.group(2) : ""));
                end = parseTime(timeColon.group(3) + (timeColon.group(4) != null ? timeColon.group(4) : ""));
            }
        }

        // 2. Fallback: match "from 1 to 2 pm" or "10 to 12" ensuring hyphen is NOT part of a date (e.g. 2026-09-28)
        if (start == null || end == null) {
            var range = java.util.regex.Pattern.compile(
                    "(?:from\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to|–|until|(?<!\\d{4}-\\d{2})-)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?"
            ).matcher(p);
            if (range.find()) {
                start = toTime(range.group(1), range.group(2), range.group(3), range.group(6));
                end = toTime(range.group(4), range.group(5), range.group(6), range.group(3));
            } else {
                var tm = java.util.regex.Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)").matcher(p);
                List<LocalTime> times = new ArrayList<>();
                while (tm.find() && times.size() < 2) {
                    times.add(toTime(tm.group(1), tm.group(2), tm.group(3), null));
                }
                if (times.size() >= 1) start = times.get(0);
                if (times.size() >= 2) end = times.get(1);
            }
        }

        // 3. College daytime adjustments for 1:00 to 7:00 (which represent 13:00 to 19:00)
        if (start != null && start.getHour() > 0 && start.getHour() < 8) {
            start = start.plusHours(12);
        }
        if (end != null && end.getHour() > 0 && end.getHour() < 8) {
            end = end.plusHours(12);
        }
        if (start != null && end != null && !end.isAfter(start) && end.getHour() < 12) {
            end = end.plusHours(12);
        }

        List<String> fac = new ArrayList<>();
        if (p.contains("projector")) fac.add("PROJECTOR");
        if (p.contains("smart")) fac.add("SMART_BOARD");
        if (p.contains("ac") || p.contains("air")) fac.add("AC");
        if (p.contains("wifi") || p.contains("wi-fi")) fac.add("WIFI");

        String building = null;
        if (p.contains("block a") || p.contains("loyalty 1")) building = "LOYALTY1";
        if (p.contains("block b") || p.contains("loyalty 2")) building = "LOYALTY2";
        if (p.contains("block c") || p.contains("loyalty 3")) building = "LOYALTY3";
        if (p.contains("block d") || p.contains("loyalty 4")) building = "LOYALTY4";
        if (p.contains("central block") || (p.contains("wisdom block") || (p.contains("wisdom") && !p.contains("seminar")))) building = "WISDOM";
        if (p.contains("siemens") || (p.contains("honesty block") || (p.contains("honesty") && !p.contains("seminar")))) building = "HONESTY";
        if (p.contains("truth") || p.contains("university block")) building = "TRUTH";

        if (resourceName == null) {
            resourceName = extractResourceName(prompt, p);
        }
        if (resourceName != null) {
            final String targetName = resourceName;
            var resOpt = resourceRepository.findByNameIgnoreCase(targetName)
                    .or(() -> resourceRepository.findByCodeIgnoreCase(targetName));
            if (resOpt.isPresent()) {
                Resource r = resOpt.get();
                if (building == null && r.getBuilding() != null) {
                    building = r.getBuilding().getCode();
                }
                if (type == null && r.getResourceType() != null) {
                    type = r.getResourceType().getCode();
                }
            }
        }

        String intent = p.contains("book") || p.contains("reserve") || p.contains("schedule") ? "BOOK"
                : p.contains("where") ? "LOCATE" : "FIND";
        String query = resourceName;

        return new Intent(type, cap, date == null ? null : date.toString(),
                start == null ? null : start.toString(), end == null ? null : end.toString(),
                fac, building, query, intent, resourceName);
    }

    private String extractResourceName(String prompt, String p) {
        List<Resource> all = resourceRepository.findAllEnabledDetailed();
        List<Resource> sorted = all.stream()
                .sorted(Comparator.comparingInt((Resource r) -> r.getName().length()).reversed())
                .toList();
        for (Resource r : sorted) {
            if (p.contains(r.getName().toLowerCase(Locale.ROOT))) {
                return r.getName();
            }
        }
        for (Resource r : all) {
            if (p.contains(r.getCode().toLowerCase(Locale.ROOT))) {
                return r.getName();
            }
        }
        var code = java.util.regex.Pattern.compile("\\b([A-Z]{1,4}-[A-Z0-9]{2,8})\\b").matcher(prompt);
        if (code.find()) {
            String c = code.group(1);
            var found = resourceRepository.findByCodeIgnoreCase(c);
            if (found.isPresent()) {
                return found.get().getName();
            }
            return c;
        }
        if (p.contains("conference room")) return "Conference Room";
        if (p.contains("central library")) return "Central Library";
        if (p.contains("digital library")) return "Digital Library";
        if (p.contains("university auditorium") || p.contains("auditorium")) return "University Auditorium";

        var named = java.util.regex.Pattern.compile(
                "(?:classroom|lab|hall|room)\\s+([a-z0-9-]{1,20})",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(prompt);
        if (named.find()) {
            String word = named.group(1).toLowerCase(Locale.ROOT);
            Set<String> stopWords = Set.of("tomorrow", "today", "yesterday", "morning", "afternoon", "evening",
                    "from", "to", "for", "with", "at", "in", "on", "by", "until", "next", "this", "a", "an", "the");
            if (!stopWords.contains(word)) {
                return named.group(0).trim();
            }
        }
        return null;
    }

    private LocalTime toTime(String hour, String minute, String meridian, String otherMeridian) {
        try {
            int h = Integer.parseInt(hour);
            int min = minute == null ? 0 : Integer.parseInt(minute);
            String ap = meridian == null ? otherMeridian : meridian;
            if (ap != null && ap.equalsIgnoreCase("pm") && h < 12) h += 12;
            if (ap != null && ap.equalsIgnoreCase("am") && h == 12) h = 0;
            if (ap == null && h > 0 && h < 8) h += 12;
            if (h <= 23 && min <= 59) {
                return LocalTime.of(h, min);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String searchableQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.length() > 32 || trimmed.split("\\s+").length > 4) {
            return null;
        }
        return trimmed;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String t = raw.trim().toLowerCase(Locale.ROOT).replace('.', ':');
        boolean pm = t.contains("pm");
        boolean am = t.contains("am");
        t = t.replace("pm", "").replace("am", "").trim();
        try {
            if (t.matches("\\d{1,2}")) {
                t = t + ":00";
            }
            if (t.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
                t = t.substring(0, t.lastIndexOf(':'));
            }
            String[] parts = t.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (pm && hour < 12) hour += 12;
            if (am && hour == 12) hour = 0;
            if (hour == 24) hour = 0;
            return LocalTime.of(hour, min);
        } catch (Exception ex) {
            return null;
        }
    }
}
