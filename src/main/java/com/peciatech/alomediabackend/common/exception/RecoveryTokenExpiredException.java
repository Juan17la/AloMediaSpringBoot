package com.peciatech.alomediabackend.common.exception;

public class RecoveryTokenExpiredException extends RuntimeException {

    public RecoveryTokenExpiredException(String message) {
        super(message);
    }
}
