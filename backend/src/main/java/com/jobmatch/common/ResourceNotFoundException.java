package com.jobmatch.common;

/** Thrown when a requested resource doesn't exist (or isn't visible to the caller). Mapped to 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
