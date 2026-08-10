package com.jobmatch.common;

/**
 * Thrown when registration is attempted with an email that is already taken.
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
