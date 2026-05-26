package com.javaeasybank.account.exception;

public class TargetAccountFailedException extends TransferException {
    public TargetAccountFailedException(String code, String message) {
        super(code, message);
    }
}
