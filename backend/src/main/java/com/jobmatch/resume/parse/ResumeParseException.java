package com.jobmatch.resume.parse;

/** Thrown when a file can't be parsed (corrupt / unreadable). Mapped to 422 by the handler. */
public class ResumeParseException extends RuntimeException {

    public ResumeParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
