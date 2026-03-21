package com.peciatech.alomediabackend.report;

import com.peciatech.alomediabackend.report.dto.response.ReportData;

import java.util.LinkedHashMap;
import java.util.Map;

public class SummaryReportFactory extends ReportFactory {

    @Override
    public String getFormat() {
        return "SUMMARY";
    }

    @Override
    public Object produce(ReportData data) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers", data.getTotalUsers());
        summary.put("totalProjects", data.getTotalProjects());
        summary.put("generatedAt", data.getGeneratedAt());
        return summary;
    }
}
