package com.peciatech.alomediabackend.report;

import com.peciatech.alomediabackend.common.exception.InvalidReportFormatException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReportFactoryProvider {

    private final Map<String, ReportFactory> factories = Map.of(
            "JSON",    new JsonReportFactory(),
            "CSV",     new CsvReportFactory(),
            "SUMMARY", new SummaryReportFactory()
    );

    public ReportFactory getFactory(String format) {
        ReportFactory factory = factories.get(format.toUpperCase());
        if (factory == null) {
            throw new InvalidReportFormatException(format);
        }
        return factory;
    }
}
