package com.jobmatch.resume.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default storage adapter — keeps files in a map. Used for local dev and tests so the
 * resume feature works end-to-end without Supabase credentials. Active unless
 * {@code resume.storage.provider=supabase}.
 */
@Component
@ConditionalOnProperty(name = "resume.storage.provider", havingValue = "memory", matchIfMissing = true)
public class InMemoryResumeStorage implements ResumeStorage {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public void upload(String key, String contentType, byte[] content) {
        store.put(key, content.clone());
    }

    @Override
    public byte[] download(String key) {
        byte[] content = store.get(key);
        if (content == null) {
            throw new StorageException("No object stored under key: " + key);
        }
        return content.clone();
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }
}
