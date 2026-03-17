package com.peciatech.alomediabackend.exception;

public class RecoveryTokenAlreadyUsedException extends RuntimeException {

    public RecoveryTokenAlreadyUsedException(String message) {
        super(message);
    }
}
