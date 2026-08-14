package com.jobmatch.chat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Deterministic stand-in chat model — streams a canned analysis word-by-word so the SSE plumbing
 * works in tests and local dev without an API key. Active unless {@code chat.provider=anthropic}.
 */
@Component
@ConditionalOnProperty(name = "chat.provider", havingValue = "fake", matchIfMissing = true)
public class FakeChatClient implements ChatClient {

    @Override
    public void streamCompletion(String system, String user, Consumer<String> onDelta) {
        String canned = "Overall fit: strong. Matching strengths: backend experience aligns with the role. "
                + "Gaps: consider highlighting more of the listed tools. "
                + "Suggestions: tailor your summary and quantify impact.";
        for (String word : canned.split(" ")) {
            onDelta.accept(word + " ");
        }
    }
}
