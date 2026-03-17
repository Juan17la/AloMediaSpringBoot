package com.peciatech.alomediabackend.exception;

public class RecoveryTokenExpiredException extends RuntimeException {

    public RecoveryTokenExpiredException(String message) {
        super(message);
    }
}
