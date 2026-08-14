package com.jobmatch.chat;

/** Wraps any failure talking to the chat model. Mapped to 502 for non-streaming callers. */
public class ChatException extends RuntimeException {

    public ChatException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChatException(String message) {
        super(message);
    }
}
