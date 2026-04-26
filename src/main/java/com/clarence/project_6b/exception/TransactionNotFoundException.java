package com.clarence.project_6b.exception;

public class TransactionNotFoundException extends ResourceNotFoundException {
    public TransactionNotFoundException(String message) {
        super(message);
    }
}
