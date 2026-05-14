package com.peciatech.alomediabackend.report;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReportFactoryProvider {

    private final Map<ReportFormat, ReportFactory> factories = Map.of(
            ReportFormat.JSON,    new JsonReportFactory(),
            ReportFormat.CSV,     new CsvReportFactory(),
            ReportFormat.SUMMARY, new SummaryReportFactory()
    );

    public ReportFactory getFactory(ReportFormat format) {
        ReportFactory factory = factories.get(format);
        if (factory == null) {
            throw new IllegalStateException("No factory registered for format: " + format);
        }
        return factory;
    }
}
