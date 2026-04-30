package com.clarence.bank_api.exception;

public class TransactionNotFoundException extends ResourceNotFoundException {
    public TransactionNotFoundException(String message) {
        super(message);
    }
}
