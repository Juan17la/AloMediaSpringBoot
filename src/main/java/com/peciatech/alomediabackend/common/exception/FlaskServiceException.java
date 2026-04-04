package com.peciatech.alomediabackend.common.exception;

import org.springframework.http.HttpStatusCode;

public class FlaskServiceException extends RuntimeException {

    private final HttpStatusCode flaskStatus;

    public FlaskServiceException(String message, HttpStatusCode flaskStatus) {
        super(message);
        this.flaskStatus = flaskStatus;
    }

    public HttpStatusCode getFlaskStatus() {
        return flaskStatus;
    }
}
