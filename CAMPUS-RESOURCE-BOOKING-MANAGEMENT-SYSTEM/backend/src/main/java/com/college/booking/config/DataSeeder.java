package com.college.booking.config;

import com.college.booking.entity.Booking;
import com.college.booking.entity.BookingApproval;
import com.college.booking.entity.BookingResource;
import com.college.booking.entity.Building;
import com.college.booking.entity.Equipment;
import com.college.booking.entity.Facility;
import com.college.booking.entity.Floor;
import com.college.booking.entity.Issue;
import com.college.booking.entity.Maintenance;
import com.college.booking.entity.Notification;
import com.college.booking.entity.Resource;
import com.college.booking.entity.ResourceFacility;
import com.college.booking.entity.ResourceType;
import com.college.booking.entity.User;
import com.college.booking.enums.ApprovalStatus;
import com.college.booking.enums.BookingStatus;
import com.college.booking.enums.BuildingKind;
import com.college.booking.enums.IssueCategory;
import com.college.booking.enums.IssueStatus;
import com.college.booking.enums.NotificationType;
import com.college.booking.enums.RecurrenceType;
import com.college.booking.enums.ResourceKind;
import com.college.booking.enums.ResourceStatus;
import com.college.booking.enums.Role;
import com.college.booking.repository.BookingApprovalRepository;
import com.college.booking.repository.BookingRepository;
import com.college.booking.repository.BookingResourceRepository;
import com.college.booking.repository.BuildingRepository;
import com.college.booking.repository.EquipmentRepository;
import com.college.booking.repository.FacilityRepository;
import com.college.booking.repository.FloorRepository;
import com.college.booking.repository.IssueRepository;
import com.college.booking.repository.MaintenanceRepository;
import com.college.booking.repository.NotificationRepository;
import com.college.booking.repository.ResourceFacilityRepository;
import com.college.booking.repository.ResourceRepository;
import com.college.booking.repository.ResourceTypeRepository;
import com.college.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("!test")
@Order(10)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final FacilityRepository facilityRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceFacilityRepository resourceFacilityRepository;
    private final BookingRepository bookingRepository;
    private final BookingResourceRepository bookingResourceRepository;
    private final BookingApprovalRepository bookingApprovalRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final IssueRepository issueRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    private final Map<String, ResourceType> types = new HashMap<>();
    private final Map<String, Facility> facilities = new HashMap<>();

    public DataSeeder(UserRepository userRepository, BuildingRepository buildingRepository,
                      FloorRepository floorRepository, ResourceTypeRepository resourceTypeRepository,
                      FacilityRepository facilityRepository, ResourceRepository resourceRepository,
                      ResourceFacilityRepository resourceFacilityRepository, BookingRepository bookingRepository,
                      BookingResourceRepository bookingResourceRepository,
                      BookingApprovalRepository bookingApprovalRepository,
                      EquipmentRepository equipmentRepository, MaintenanceRepository maintenanceRepository,
                      IssueRepository issueRepository, NotificationRepository notificationRepository,
                      PasswordEncoder passwordEncoder, @Value("${app.demo-password}") String demoPassword) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.facilityRepository = facilityRepository;
        this.resourceRepository = resourceRepository;
        this.resourceFacilityRepository = resourceFacilityRepository;
        this.bookingRepository = bookingRepository;
        this.bookingResourceRepository = bookingResourceRepository;
        this.bookingApprovalRepository = bookingApprovalRepository;
        this.equipmentRepository = equipmentRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.issueRepository = issueRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        seedTypes();
        seedFacilities();
        User admin = user("Campus Administrator", "admin@example.com", Role.ADMIN, "Administration", null);
        User professor = user("Dr. Priya Sharma", "professor@example.com", Role.PROFESSOR, "CSE", null);
        User student = user("Akhil Reddy", "student@example.com", Role.STUDENT, "CSE", professor);
        user("Dr. Ramesh Rao", "ramesh.rao@example.com", Role.PROFESSOR, "ECE", null);
        user("Ananya Patel", "ananya.patel@example.com", Role.STUDENT, "CSE", professor);

        Building ground = building("Sports Ground", "GROUND", "Ground",
                "Outdoor sports field. Bookings are not accepted on this area.",
                BuildingKind.SPORTS, false, "Athletics", 26.0, 30.5, 16.0, 20.0);
        Building blockB = building("Block B", "LOYALTY2", "Loyalty 2",
                "North academic wing of the main quadrangle. Electronics and communication classrooms and labs.",
                BuildingKind.ACADEMIC, true, "ECE", 40.0, 36.0, 16.5, 11.0);
        Building central = building("Central Block", "WISDOM", "Wisdom",
                "Heart of the campus quadrangle. Houses the central seminar hall and the library stacks — no regular classrooms.",
                BuildingKind.CENTRAL, true, "Library", 47.2, 42.0, 8.0, 11.0);
        Building blockC = building("Block C", "LOYALTY3", "Loyalty 3",
                "North-east academic cluster with mechanical and civil engineering spaces.",
                BuildingKind.ACADEMIC, true, "Mechanical", 63.0, 30.0, 17.0, 16.0);
        Building blockA = building("Block A", "LOYALTY1", "Loyalty 1",
                "South-west academic wing. Home of Computer Science classrooms and computing laboratories.",
                BuildingKind.ACADEMIC, true, "CSE", 39.5, 48.0, 8.5, 11.0);
        Building blockD = building("Block D", "LOYALTY4", "Loyalty 4",
                "South-east academic wing for sciences and applied laboratories.",
                BuildingKind.ACADEMIC, true, "Sciences", 54.8, 48.0, 9.0, 11.0);
        Building truth = building("University Block", "TRUTH", "Truth",
                "Administrative and examination block west of Honesty. Auditorium and exam halls.",
                BuildingKind.ADMIN, true, "Administration", 20.0, 64.0, 12.0, 18.0);
        Building siemens = building("Siemens Block", "HONESTY", "Honesty",
                "Industry collaboration block with the Siemens Centre of Excellence, workshops and solar-roofed halls.",
                BuildingKind.INDUSTRY, true, "Industry", 38.5, 58.0, 34.0, 20.0);

        seedAcademic(blockA, "A", "CSE", 4, new String[]{"Python Lab", "Networks Lab"});
        seedAcademic(blockB, "B", "ECE", 3, new String[]{"VLSI Lab", "Communications Lab"});
        seedAcademic(blockC, "C", "Mechanical", 4, new String[]{"CAD/CAM Lab", "Fluid Mechanics Lab"});
        seedAcademic(blockD, "D", "Sciences", 3, new String[]{"Physics Lab", "Chemistry Lab"});
        seedCentral(central);
        seedTruth(truth);
        seedSiemens(siemens);
        seedGround(ground);

        seedEquipment();
        seedBookings(student, professor, admin);
        seedOps(admin, student, professor);
        notify(student, "Welcome to CampusOS", "Explore the VVIT digital campus and book Loyalty 1 in a few clicks.", NotificationType.SYSTEM, "/campus");
        notify(professor, "Approvals waiting", "Student booking requests are ready for review.", NotificationType.PROFESSOR_APPROVAL, "/approvals");
        notify(admin, "Campus seeded", "Buildings, floors and resources were loaded from the VVIT campus map.", NotificationType.SYSTEM, "/admin");
    }

    private void seedTypes() {
        type("CLASSROOM", "Classroom", ResourceKind.CLASSROOM, false);
        type("LABORATORY", "Laboratory", ResourceKind.LABORATORY, false);
        type("SEMINAR_HALL", "Seminar Hall", ResourceKind.SEMINAR_HALL, false);
        type("LIBRARY", "Library", ResourceKind.LIBRARY, false);
        type("AUDITORIUM", "Auditorium", ResourceKind.AUDITORIUM, false);
        type("EXAMINATION_HALL", "Examination Hall", ResourceKind.EXAMINATION_HALL, false);
        type("SPORTS_FACILITY", "Sports Facility", ResourceKind.SPORTS_FACILITY, false);
        type("EQUIPMENT", "Equipment", ResourceKind.EQUIPMENT, false);
    }

    private void seedFacilities() {
        fac("PROJECTOR", "Projector");
        fac("SMART_BOARD", "Smart Board");
        fac("AC", "Air Conditioning");
        fac("WIFI", "Wi-Fi");
        fac("AUDIO", "Audio System");
        fac("MIC", "Microphones");
        fac("STAGE", "Stage");
        fac("COMPUTERS", "Computers");
        fac("WHITEBOARD", "Whiteboard");
    }

    private void seedAcademic(Building building, String prefix, String dept, int floors, String[] labNames) {
        for (int level = 0; level < floors; level++) {
            String fname = level == 0 ? "Ground Floor" : (level == 1 ? "First Floor" : level == 2 ? "Second Floor" : "Third Floor");
            Floor floor = floor(building, fname, level);
            for (int i = 1; i <= 5; i++) {
                int roomNum = level * 100 + i;
                String code = prefix + "-" + String.format("%03d", roomNum == 0 ? i : roomNum);
                if (level == 0) code = prefix + "-G0" + i;
                Resource room = resource(floor, code, "Classroom " + code, "CLASSROOM", dept,
                        40 + (i % 3) * 10, 4 + (i - 1) * 19, 8, 16.5, 22,
                        true, i % 2 == 0, true, true, false, false, false, null, null);
                attach(room, "PROJECTOR", "WIFI", "AC", "WHITEBOARD");
                if (i % 2 == 0) attach(room, "SMART_BOARD");
            }
            Resource lab1 = resource(floor, prefix + "-L" + level + "1", labNames[0] + " " + fname, "LABORATORY", dept,
                    30, 8, 40, 28, 26, true, true, true, true, false, false, false, 30, "Workstations and licensed IDEs");
            Resource lab2 = resource(floor, prefix + "-L" + level + "2", labNames[1] + " " + fname, "LABORATORY", dept,
                    32, 42, 40, 28, 26, true, false, true, true, false, false, false, 32, "Specialized lab equipment");
            attach(lab1, "COMPUTERS", "AC", "WIFI", "PROJECTOR");
            attach(lab2, "COMPUTERS", "AC", "WIFI");
            if (level == 0) {
                Resource hall = resource(floor, prefix + "-SH", prefix + " Seminar Hall", "SEMINAR_HALL", dept,
                        180, 8, 72, 70, 22, true, true, true, true, true, true, true, null, "PA system and stage lighting");
                attach(hall, "PROJECTOR", "AUDIO", "MIC", "AC", "STAGE", "WIFI");
            }
        }
    }

    private void seedCentral(Building building) {
        Floor f1 = floor(building, "1st Floor", 1);
        Floor f2 = floor(building, "2nd Floor", 2);
        Floor f3 = floor(building, "3rd Floor", 3);
        Resource hall = resource(f1, "WIS-SH", "Wisdom Seminar Hall", "SEMINAR_HALL", "Library",
                220, 10, 18, 80, 64, true, true, true, true, true, true, true, null, "Central gathering hall");
        attach(hall, "PROJECTOR", "AUDIO", "MIC", "AC", "STAGE", "WIFI", "SMART_BOARD");
        Resource lib2 = resource(f2, "WIS-LIB2", "Central Library", "LIBRARY", "Library",
                240, 8, 12, 84, 76, false, false, true, true, false, false, false, null, "Reading rooms and stacks");
        lib2.setStudySeats(180);
        lib2.setReadingArea(true);
        lib2.setOpeningHours("08:00–18:00");
        resourceRepository.save(lib2);
        attach(lib2, "WIFI", "AC");
        Resource lib3 = resource(f3, "WIS-LIB3", "Digital Library", "LIBRARY", "Library",
                120, 8, 12, 84, 76, true, true, true, true, false, false, false, 80, "e-resources and research cubicles");
        lib3.setStudySeats(80);
        lib3.setReadingArea(true);
        lib3.setOpeningHours("08:00–20:00");
        resourceRepository.save(lib3);
        attach(lib3, "WIFI", "AC", "COMPUTERS", "PROJECTOR");
    }

    private void seedTruth(Building building) {
        Floor g = floor(building, "Ground Floor", 0);
        Floor f1 = floor(building, "First Floor", 1);
        Floor f2 = floor(building, "Second Floor", 2);
        Resource aud = resource(g, "TR-AUD", "University Auditorium", "AUDITORIUM", "Administration",
                500, 8, 10, 84, 50, true, true, true, true, true, true, true, null, "Main campus auditorium");
        attach(aud, "PROJECTOR", "AUDIO", "MIC", "AC", "STAGE", "WIFI");
        Resource exam1 = resource(f1, "TR-EX1", "Examination Hall 1", "EXAMINATION_HALL", "Examination",
                200, 6, 12, 42, 70, false, false, true, false, false, false, false, null, "Row seating");
        exam1.setSeatingArrangement("Rows");
        resourceRepository.save(exam1);
        Resource exam2 = resource(f1, "TR-EX2", "Examination Hall 2", "EXAMINATION_HALL", "Examination",
                180, 52, 12, 42, 70, false, false, true, false, false, false, false, null, "Row seating");
        exam2.setSeatingArrangement("Rows");
        resourceRepository.save(exam2);
        for (int i = 1; i <= 5; i++) {
            Resource room = resource(f2, "TR-2" + i, "Conference " + i, "CLASSROOM", "Administration",
                    24 + i * 4, 6 + (i - 1) * 18, 20, 16, 22, true, true, true, true, false, false, false, null, null);
            attach(room, "PROJECTOR", "AC", "WIFI");
        }
    }

    private void seedSiemens(Building building) {
        String[] labs = {"Automation Lab", "PLC Lab", "Mechatronics Lab", "Industry 4.0 Lab"};
        for (int level = 0; level < 4; level++) {
            String fname = level == 0 ? "Ground Floor" : (level + " Floor");
            Floor floor = floor(building, fname, level);
            for (int i = 1; i <= 5; i++) {
                String code = "H-" + level + "0" + i;
                Resource room = resource(floor, code, "Honesty Classroom " + code, "CLASSROOM", "Industry",
                        50, 4 + (i - 1) * 19, 8, 16.5, 22, true, true, true, true, false, false, false, null, null);
                attach(room, "PROJECTOR", "AC", "WIFI", "SMART_BOARD");
            }
            Resource lab1 = resource(floor, "H-L" + level + "1", labs[level] + " A", "LABORATORY", "Industry",
                    28, 8, 40, 28, 26, true, true, true, true, false, false, false, 28, "Siemens industrial kits");
            Resource lab2 = resource(floor, "H-L" + level + "2", labs[level] + " B", "LABORATORY", "Industry",
                    28, 42, 40, 28, 26, true, false, true, true, false, false, false, 24, "Hardware benches");
            attach(lab1, "COMPUTERS", "AC", "WIFI", "PROJECTOR");
            attach(lab2, "COMPUTERS", "AC", "WIFI");
            if (level == 0) {
                Resource hall = resource(floor, "H-SH", "Honesty Seminar Hall", "SEMINAR_HALL", "Industry",
                        160, 8, 72, 70, 22, true, true, true, true, true, true, true, null, "Industry guest lectures");
                attach(hall, "PROJECTOR", "AUDIO", "MIC", "AC", "STAGE", "WIFI");
            }
        }
    }

    private void seedGround(Building building) {
        Floor field = floor(building, "Outdoor", 0);
        Resource pitch = resource(field, "GR-FIELD", "Main Playground", "SPORTS_FACILITY", "Athletics",
                200, 10, 10, 80, 80, false, false, false, false, false, false, false, null, "Not bookable — outdoor ground");
        pitch.setSportsType("Multi-sport field");
        pitch.setEnabled(false);
        pitch.setOperationalStatus(ResourceStatus.BLOCKED);
        resourceRepository.save(pitch);
    }

    private void seedEquipment() {
        eq("Portable Projector", "PROJECTOR", 12, "Epson classroom projectors");
        eq("Laptop", "LAPTOP", 20, "Dell Latitude pool");
        eq("Camera", "CAMERA", 6, "Event cameras");
        eq("Microphone", "MICROPHONE", 15, "Wireless handheld");
        eq("Speaker", "SPEAKER", 8, "PA speakers");
        eq("Extension Cable", "OTHER", 20, "Power distribution");
    }

    private void seedBookings(User student, User professor, User admin) {
        LocalDate today = LocalDate.now();
        Resource aRoom = resourceRepository.findByCode("A-101").or(() -> resourceRepository.findByCode("A-G01")).orElse(
                resourceRepository.findAll().stream().filter(r -> r.getCode().startsWith("A-")).findFirst().orElseThrow());
        Resource hall = resourceRepository.findByCode("WIS-SH").orElseThrow();
        Resource lab = resourceRepository.findByCode("A-L11").or(() -> resourceRepository.findByCode("A-L01")).orElse(
                resourceRepository.findAll().stream().filter(r -> r.getCode().contains("-L")).findFirst().orElseThrow());

        Booking pending = booking(student, "Project discussion", "Capstone review", today.plusDays(1),
                LocalTime.of(14, 0), LocalTime.of(16, 0), BookingStatus.PENDING_PROFESSOR, 12, aRoom);
        approval(pending, Role.PROFESSOR, ApprovalStatus.PENDING, null);
        approval(pending, Role.ADMIN, ApprovalStatus.PENDING, null);

        Booking confirmed = booking(student, "Lab practice", "Compiler lab extra hour", today,
                LocalTime.of(10, 0), LocalTime.of(12, 0), BookingStatus.CONFIRMED, 28, lab);
        approval(confirmed, Role.PROFESSOR, ApprovalStatus.APPROVED, professor);
        approval(confirmed, Role.ADMIN, ApprovalStatus.APPROVED, admin);

        Booking guest = booking(professor, "Guest lecture", "Industry talk", today.plusDays(2),
                LocalTime.of(10, 0), LocalTime.of(13, 0), BookingStatus.PENDING_ADMIN, 160, hall);
        approval(guest, Role.ADMIN, ApprovalStatus.PENDING, null);

        for (int d = 1; d <= 10; d++) {
            Resource r = resourceRepository.findAll().get(d + 3);
            Booking past = booking(student, "Study group " + d, "Peer learning", today.minusDays(d),
                    LocalTime.of(9, 0), LocalTime.of(11, 0),
                    d % 5 == 0 ? BookingStatus.CANCELLED : BookingStatus.COMPLETED, 18, r);
            approval(past, Role.PROFESSOR, ApprovalStatus.APPROVED, professor);
            approval(past, Role.ADMIN, ApprovalStatus.APPROVED, admin);
        }
    }

    private void seedOps(User admin, User student, User professor) {
        Resource target = resourceRepository.findByCode("A-102").or(() -> resourceRepository.findByCode("A-G02"))
                .orElse(resourceRepository.findAll().get(2));
        Maintenance m = new Maintenance();
        m.setResource(target);
        m.setStartDate(LocalDate.now().plusDays(5));
        m.setEndDate(LocalDate.now().plusDays(6));
        m.setReason("Projector lamp replacement");
        m.setActive(true);
        maintenanceRepository.save(m);

        Issue issue = new Issue();
        issue.setResource(target);
        issue.setReporter(student);
        issue.setCategory(IssueCategory.AC);
        issue.setDescription("AC not cooling on the south side of the room.");
        issue.setStatus(IssueStatus.ASSIGNED);
        issue.setAssignee(admin);
        issueRepository.save(issue);
    }

    private User user(String name, String email, Role role, String dept, User professor) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(demoPassword));
        u.setRole(role);
        u.setDepartment(dept);
        u.setEnabled(true);
        u.setNoShowCount(0);
        u.setAssignedProfessor(professor);
        return userRepository.save(u);
    }

    private Building building(String name, String code, String virtue, String desc, BuildingKind kind,
                              boolean bookable, String dept, double x, double y, double w, double h) {
        Building b = new Building();
        b.setName(name);
        b.setCode(code);
        b.setVirtueName(virtue);
        b.setDescription(desc);
        b.setKind(kind);
        b.setBookable(bookable);
        b.setDepartment(dept);
        b.setMapX(x);
        b.setMapY(y);
        b.setMapWidth(w);
        b.setMapHeight(h);
        b.setImageUrl("/campus.jpg");
        return buildingRepository.save(b);
    }

    private Floor floor(Building building, String name, int level) {
        Floor f = new Floor();
        f.setBuilding(building);
        f.setName(name);
        f.setLevel(level);
        f.setDescription(name + " of " + building.getName());
        return floorRepository.save(f);
    }

    private Resource resource(Floor floor, String code, String name, String type, String dept, int capacity,
                              double x, double y, double w, double h,
                              boolean projector, boolean smart, boolean ac, boolean wifi,
                              boolean audio, boolean mic, boolean stage, Integer computers, String notes) {
        Resource r = new Resource();
        r.setFloor(floor);
        r.setBuilding(floor.getBuilding());
        r.setCode(code);
        r.setName(name);
        r.setResourceType(types.get(type));
        r.setDepartment(dept);
        r.setCapacity(capacity);
        r.setPositionX(x);
        r.setPositionY(y);
        r.setWidth(w);
        r.setHeight(h);
        r.setRotation(0d);
        r.setProjector(projector);
        r.setSmartBoard(smart);
        r.setAirConditioned(ac);
        r.setWifi(wifi);
        r.setAudio(audio);
        r.setMicrophones(mic);
        r.setStage(stage);
        r.setComputers(computers);
        r.setEquipmentNotes(notes);
        r.setOperationalStatus(ResourceStatus.AVAILABLE);
        r.setEnabled(true);
        r.setWorkingHoursStart(LocalTime.of(8, 0));
        r.setWorkingHoursEnd(LocalTime.of(18, 0));
        r.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        r.setDescription(name + " in " + floor.getBuilding().getName() + " · " + floor.getName());
        return resourceRepository.save(r);
    }

    private void attach(Resource resource, String... codes) {
        for (String code : codes) {
            ResourceFacility rf = new ResourceFacility();
            rf.setResource(resource);
            rf.setFacility(facilities.get(code));
            resourceFacilityRepository.save(rf);
        }
    }

    private void type(String code, String name, ResourceKind kind, boolean custom) {
        ResourceType t = new ResourceType();
        t.setCode(code);
        t.setName(name);
        t.setKind(kind);
        t.setCustom(custom);
        types.put(code, resourceTypeRepository.save(t));
    }

    private void fac(String code, String name) {
        Facility f = new Facility();
        f.setCode(code);
        f.setName(name);
        facilities.put(code, facilityRepository.save(f));
    }

    private void eq(String name, String type, int qty, String desc) {
        Equipment e = new Equipment();
        e.setName(name);
        e.setType(type);
        e.setQuantity(qty);
        e.setAvailable(qty);
        e.setDescription(desc);
        e.setEnabled(true);
        equipmentRepository.save(e);
    }

    private Booking booking(User user, String title, String purpose, LocalDate date, LocalTime start, LocalTime end,
                            BookingStatus status, int attendees, Resource resource) {
        Booking b = new Booking();
        b.setUser(user);
        b.setTitle(title);
        b.setPurpose(purpose);
        b.setBookingDate(date);
        b.setStartTime(start);
        b.setEndTime(end);
        b.setStatus(status);
        b.setAttendees(attendees);
        b.setRecurrenceType(RecurrenceType.NONE);
        b.setBookingKind("RESOURCE");
        b.setCheckInToken(UUID.randomUUID().toString());
        bookingRepository.save(b);
        BookingResource br = new BookingResource();
        br.setBooking(b);
        br.setResource(resource);
        bookingResourceRepository.save(br);
        return b;
    }

    private void approval(Booking booking, Role role, ApprovalStatus status, User approver) {
        BookingApproval a = new BookingApproval();
        a.setBooking(booking);
        a.setRequiredRole(role);
        a.setStatus(status);
        a.setApprover(approver);
        bookingApprovalRepository.save(a);
    }

    private void notify(User user, String title, String message, NotificationType type, String link) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setLink(link);
        n.setReadFlag(false);
        notificationRepository.save(n);
    }
}
