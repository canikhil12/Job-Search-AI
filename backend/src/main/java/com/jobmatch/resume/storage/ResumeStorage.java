package com.jobmatch.resume.storage;

/**
 * Port for storing raw resume files. The service depends on this abstraction, not on
 * Supabase directly — so the upload flow is testable without any network call
 * (see {@code InMemoryResumeStorage}) and the backend could swap providers later.
 */
public interface ResumeStorage {

    /** Stores the given bytes under {@code key} (overwriting if it already exists). */
    void upload(String key, String contentType, byte[] content);

    /** Fetches the bytes previously stored under {@code key}. */
    byte[] download(String key);

    /** Removes the object at {@code key}. Must not throw if the object is already gone. */
    void delete(String key);
}
