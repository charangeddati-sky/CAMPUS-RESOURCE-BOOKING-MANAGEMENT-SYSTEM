package com.college.booking.controller;

import com.college.booking.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> csv(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return file(reportService.csv(from, to), "text/csv", "bookings.csv");
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return file(reportService.excel(from, to),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "bookings.xlsx");
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return file(reportService.pdf(from, to), MediaType.APPLICATION_PDF_VALUE, "bookings.pdf");
    }

    private ResponseEntity<byte[]> file(byte[] body, String type, String name) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + name)
                .contentType(MediaType.parseMediaType(type))
                .body(body);
    }
}
