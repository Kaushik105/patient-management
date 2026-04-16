package org.kaushik.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String s) {
        super(s);
    }
}
