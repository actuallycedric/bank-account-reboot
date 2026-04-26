package com.clarence.project_6b.exception;

public class AccountViolationException extends RuntimeException {
    public AccountViolationException(String message) {
        super(message);
    }
}
