package com.peciatech.alomediabackend.common.exception;

public class InvalidReportFormatException extends RuntimeException {

    public InvalidReportFormatException(String format) {
        super("Invalid report format: " + format);
    }
}
