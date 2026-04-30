package com.clarence.bank_api.exception;

public class AccountViolationException extends RuntimeException {
    public AccountViolationException(String message) {
        super(message);
    }
}
