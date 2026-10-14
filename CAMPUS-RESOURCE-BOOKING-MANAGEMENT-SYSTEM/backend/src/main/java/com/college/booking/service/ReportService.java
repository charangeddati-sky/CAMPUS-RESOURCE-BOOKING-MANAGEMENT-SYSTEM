package com.college.booking.service;

import com.college.booking.dto.AnalyticsDtos.AnalyticsOverview;
import com.college.booking.dto.AnalyticsDtos.DayCount;
import com.college.booking.dto.AnalyticsDtos.DemandForecast;
import com.college.booking.dto.AnalyticsDtos.FrequentResourceForecast;
import com.college.booking.dto.AnalyticsDtos.HourCount;
import com.college.booking.dto.AnalyticsDtos.NamedCount;
import com.college.booking.dto.AnalyticsDtos.ResourceRank;
import com.college.booking.entity.Booking;
import com.college.booking.repository.BookingRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final Color GROVE = new Color(15, 61, 46);
    private static final Color GOLD = new Color(212, 160, 23);
    private static final Color CREAM = new Color(244, 239, 228);
    private static final Color BRICK = new Color(193, 74, 50);

    private final BookingRepository bookingRepository;
    private final DashboardService dashboardService;
    private final AnalyticsService analyticsService;
    private final String collegeName;
    private final String collegeShort;

    public ReportService(BookingRepository bookingRepository, DashboardService dashboardService,
                         AnalyticsService analyticsService,
                         @Value("${app.college-name}") String collegeName,
                         @Value("${app.college-short}") String collegeShort) {
        this.bookingRepository = bookingRepository;
        this.dashboardService = dashboardService;
        this.analyticsService = analyticsService;
        this.collegeName = collegeName;
        this.collegeShort = collegeShort;
    }

    public byte[] csv(LocalDate from, LocalDate to) {
        List<Booking> bookings = bookingRepository.findBetween(from, to);
        StringBuilder sb = new StringBuilder("id,date,start,end,status,title,user,kind\n");
        for (Booking b : bookings) {
            sb.append(b.getId()).append(',')
                    .append(b.getBookingDate()).append(',')
                    .append(b.getStartTime()).append(',')
                    .append(b.getEndTime()).append(',')
                    .append(b.getStatus()).append(',')
                    .append(esc(b.getTitle())).append(',')
                    .append(esc(b.getUser().getEmail())).append(',')
                    .append(b.getBookingKind()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] excel(LocalDate from, LocalDate to) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bookings");
            Row header = sheet.createRow(0);
            String[] cols = {"ID", "Date", "Start", "End", "Status", "Title", "User", "Kind"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            int r = 1;
            for (Booking b : bookingRepository.findBetween(from, to)) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(b.getId());
                row.createCell(1).setCellValue(b.getBookingDate().toString());
                row.createCell(2).setCellValue(b.getStartTime().toString());
                row.createCell(3).setCellValue(b.getEndTime().toString());
                row.createCell(4).setCellValue(b.getStatus().name());
                row.createCell(5).setCellValue(b.getTitle());
                row.createCell(6).setCellValue(b.getUser().getEmail());
                row.createCell(7).setCellValue(b.getBookingKind());
            }
            Sheet util = wb.createSheet("Utilization");
            Row uh = util.createRow(0);
            uh.createCell(0).setCellValue("Resource");
            uh.createCell(1).setCellValue("Building");
            uh.createCell(2).setCellValue("Utilization %");
            int ur = 1;
            for (Map<String, Object> row : dashboardService.utilization(from, to)) {
                Row x = util.createRow(ur++);
                x.createCell(0).setCellValue(String.valueOf(row.get("name")));
                x.createCell(1).setCellValue(String.valueOf(row.get("building")));
                x.createCell(2).setCellValue(String.valueOf(row.get("utilization")));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build Excel report", ex);
        }
    }

    public byte[] pdf(LocalDate from, LocalDate to) {
        AnalyticsOverview analytics = analyticsService.overview(from, to);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 48);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Footer(collegeShort));
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, GROVE);
            Font section = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, GROVE);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

            PdfPTable banner = new PdfPTable(1);
            banner.setWidthPercentage(100);
            PdfPCell bannerCell = new PdfPCell(new Phrase(collegeName + "  ·  CampusOS analytics", white));
            bannerCell.setBackgroundColor(GROVE);
            bannerCell.setPadding(12);
            bannerCell.setBorder(Rectangle.NO_BORDER);
            banner.addCell(bannerCell);
            doc.add(banner);

            Paragraph heading = new Paragraph("Campus performance report", title);
            heading.setSpacingBefore(12);
            doc.add(heading);
            doc.add(new Paragraph("Period: " + from + " to " + to
                    + "    Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), small));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Key performance indicators", section));
            PdfPTable kpis = new PdfPTable(4);
            kpis.setWidthPercentage(100);
            kpis.setSpacingBefore(6);
            kpis.setSpacingAfter(10);
            addKpi(kpis, "Bookings", String.valueOf(analytics.kpis().get("totalBookings")));
            addKpi(kpis, "Active users", String.valueOf(analytics.kpis().get("activeUsers")));
            addKpi(kpis, "Avg utilization", analytics.kpis().get("avgUtilizationPercent") + "%");
            addKpi(kpis, "Live occupancy", analytics.kpis().get("occupancyPercent") + "%");
            addKpi(kpis, "Completed", String.valueOf(analytics.kpis().get("completed")));
            addKpi(kpis, "Cancelled", analytics.kpis().get("cancellationRatePercent") + "%");
            addKpi(kpis, "No-shows", analytics.kpis().get("noShowRatePercent") + "%");
            addKpi(kpis, "Open issues", String.valueOf(analytics.kpis().get("openIssues")));
            doc.add(kpis);

            doc.add(new Paragraph("Booking status mix", section));
            PdfPTable status = table(analytics.statusMix().size() == 0 ? 2 : Math.min(4, analytics.statusMix().size()));
            analytics.statusMix().forEach((k, v) -> status.addCell(cell(k.replace('_', ' ') + ": " + v, body, CREAM)));
            doc.add(status);

            doc.add(new Paragraph("Most booked resources", section));
            PdfPTable most = headerTable(new String[]{"Resource", "Building", "Bookings", "Utilization"});
            for (ResourceRank r : analytics.mostBooked()) {
                most.addCell(cell(r.name(), body, Color.WHITE));
                most.addCell(cell(r.building(), body, Color.WHITE));
                most.addCell(cell(String.valueOf(r.bookings()), body, Color.WHITE));
                most.addCell(cell(r.utilizationPercent() + "%", body, Color.WHITE));
            }
            doc.add(most);

            doc.add(new Paragraph("Building performance", section));
            PdfPTable buildings = headerTable(new String[]{"Building", "Bookings"});
            for (NamedCount n : analytics.buildingPerformance()) {
                buildings.addCell(cell(n.name(), body, Color.WHITE));
                buildings.addCell(cell(String.valueOf(n.count()), body, Color.WHITE));
            }
            doc.add(buildings);

            doc.add(new Paragraph("Peak hours (actual)", section));
            PdfPTable hours = headerTable(new String[]{"Hour", "Bookings"});
            for (HourCount h : analytics.peakHours()) {
                hours.addCell(cell(h.hour(), body, Color.WHITE));
                hours.addCell(cell(String.valueOf(h.count()), body, Color.WHITE));
            }
            doc.add(hours);

            doc.add(new Paragraph("Daily trend (actual)", section));
            PdfPTable days = headerTable(new String[]{"Date", "Weekday", "Bookings"});
            for (DayCount d : analytics.bookingTrends()) {
                days.addCell(cell(d.date(), body, Color.WHITE));
                days.addCell(cell(d.dayOfWeek(), body, Color.WHITE));
                days.addCell(cell(String.valueOf(d.count()), body, Color.WHITE));
            }
            doc.add(days);

            Paragraph predTitle = new Paragraph("Predictions (forecast — not live data)", section);
            predTitle.setSpacingBefore(8);
            doc.add(predTitle);
            doc.add(new Paragraph(analytics.predictions().disclaimer(), small));
            doc.add(new Paragraph("Method: " + analytics.predictions().method(), small));
            doc.add(new Paragraph("Predicted peak hour: " + analytics.predictions().peakHour().value()
                    + " (" + analytics.predictions().peakHour().confidence() + " confidence)", body));
            doc.add(new Paragraph("Predicted peak day: " + analytics.predictions().peakDayOfWeek().value()
                    + " (" + analytics.predictions().peakDayOfWeek().confidence() + " confidence)", body));

            PdfPTable freq = headerTable(new String[]{"Resource", "Expected / week", "Confidence"});
            for (FrequentResourceForecast f : analytics.predictions().frequentResources()) {
                freq.addCell(cell(f.name(), body, Color.WHITE));
                freq.addCell(cell(String.valueOf(f.expectedBookingsPerWeek()), body, Color.WHITE));
                freq.addCell(cell(f.confidence(), body, Color.WHITE));
            }
            doc.add(freq);

            PdfPTable demand = headerTable(new String[]{"Date", "Weekday", "Expected bookings", "Confidence"});
            for (DemandForecast d : analytics.predictions().nextSevenDays()) {
                demand.addCell(cell(d.date(), body, Color.WHITE));
                demand.addCell(cell(d.dayOfWeek(), body, Color.WHITE));
                demand.addCell(cell(String.valueOf(d.expectedBookings()), body, Color.WHITE));
                demand.addCell(cell(d.confidence(), body, Color.WHITE));
            }
            doc.add(demand);

            doc.add(new Paragraph("Booking register", section));
            PdfPTable table = headerTable(new String[]{"Date", "Time", "Title", "User", "Status", "Kind"});
            int n = 0;
            for (Booking b : bookingRepository.findBetween(from, to)) {
                if (n++ >= 200) {
                    table.addCell(span("Showing first 200 bookings.", body, 6));
                    break;
                }
                table.addCell(cell(b.getBookingDate().toString(), body, Color.WHITE));
                table.addCell(cell(b.getStartTime() + "–" + b.getEndTime(), body, Color.WHITE));
                table.addCell(cell(b.getTitle() == null ? "" : b.getTitle(), body, Color.WHITE));
                table.addCell(cell(b.getUser().getEmail(), body, Color.WHITE));
                table.addCell(cell(b.getStatus().name(), body, Color.WHITE));
                table.addCell(cell(b.getBookingKind(), body, Color.WHITE));
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build PDF report", ex);
        }
    }

    private void addKpi(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 7, GOLD);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, GROVE);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(CREAM);
        cell.setPadding(8);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Phrase(label.toUpperCase(), labelFont));
        cell.addElement(new Phrase(value == null ? "—" : value, valueFont));
        table.addCell(cell);
    }

    private PdfPTable headerTable(String[] headers) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(10);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(GROVE);
            cell.setPadding(5);
            table.addCell(cell);
        }
        return table;
    }

    private PdfPTable table(int cols) {
        PdfPTable t = new PdfPTable(Math.max(cols, 1));
        t.setWidthPercentage(100);
        t.setSpacingBefore(6);
        t.setSpacingAfter(10);
        return t;
    }

    private PdfPCell cell(String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell span(String text, Font font, int cols) {
        PdfPCell cell = cell(text, font, CREAM);
        cell.setColspan(cols);
        return cell;
    }

    private String esc(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static final class Footer extends PdfPageEventHelper {
        private final String shortName;

        Footer(String shortName) {
            this.shortName = shortName;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Phrase phrase = new Phrase(shortName + " CampusOS  ·  confidential  ·  page " + writer.getPageNumber(), font);
            com.lowagie.text.pdf.ColumnText.showTextAligned(
                    writer.getDirectContent(), Element.ALIGN_CENTER,
                    phrase, (document.right() + document.left()) / 2, document.bottom() - 20, 0);
        }
    }
}
