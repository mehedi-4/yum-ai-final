package com.yumai.controller;

import com.yumai.entity.Report;
import com.yumai.entity.Report.ReportFormat;
import com.yumai.entity.Report.ReportType;
import com.yumai.entity.User;
import com.yumai.service.ReportService;
import com.yumai.service.ReportService.ReportFile;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** FR-06 / UC-11, UC-12 - report export (Manager/Admin only, FR-06.4). */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/history")
    public List<Report> history() {
        return reportService.history();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam ReportType type,
            @RequestParam ReportFormat format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long menuItemId,
            @AuthenticationPrincipal User user) {
        ReportFile file = reportService.generate(type, format, from, to, category, menuItemId, user);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.filename())
                .body(file.bytes());
    }
}
