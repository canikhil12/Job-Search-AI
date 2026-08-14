package com.jobmatch.chat;

import java.util.function.Consumer;

/**
 * Port for streaming chat completions. The concrete adapter is chosen by {@code chat.provider}:
 * Anthropic in prod, a deterministic fake for tests/local (so streaming works without an API key).
 */
public interface ChatClient {

    /**
     * Streams a completion for the given system + user prompt, invoking {@code onDelta} for each
     * text chunk as it arrives. Blocks until the stream finishes; throws {@link ChatException} on failure.
     */
    void streamCompletion(String system, String user, Consumer<String> onDelta);

    /** Returns the full completion for the given prompt (non-streaming) — used for structured output. */
    String complete(String system, String user);
}
