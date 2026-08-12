package com.jobmatch.resume;

/** Thrown when an uploaded file isn't an accepted resume type. Mapped to 415. */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
