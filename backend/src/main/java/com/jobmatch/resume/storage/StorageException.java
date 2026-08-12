package com.jobmatch.resume.storage;

/** Wraps any failure talking to the storage backend. Mapped to 502 by the exception handler. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}
