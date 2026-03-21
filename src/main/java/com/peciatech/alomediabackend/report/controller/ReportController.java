package com.peciatech.alomediabackend.report.controller;

import com.peciatech.alomediabackend.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<?> getReport(@RequestParam(defaultValue = "JSON") String format) {
        Object result = reportService.generateReport(format);

        if ("CSV".equalsIgnoreCase(format)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.csv\"");
            return ResponseEntity.ok().headers(headers).body((String) result);
        }

        return ResponseEntity.ok(result);
    }
}
