package com.peciatech.alomediabackend.common.exception;

public class RecoveryTokenAlreadyUsedException extends RuntimeException {

    public RecoveryTokenAlreadyUsedException(String message) {
        super(message);
    }
}
