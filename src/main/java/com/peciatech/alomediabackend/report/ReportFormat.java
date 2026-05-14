package com.peciatech.alomediabackend.report;

import com.peciatech.alomediabackend.common.exception.InvalidReportFormatException;

public enum ReportFormat {
    JSON,
    CSV,
    SUMMARY;

    public static ReportFormat fromString(String value) {
        if (value == null || value.isBlank()) {
            return JSON;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidReportFormatException(value);
        }
    }
}
