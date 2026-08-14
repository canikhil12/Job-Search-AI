package com.jobmatch;

import com.jobmatch.chat.FakeChatClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FakeChatClientTest {

    private final FakeChatClient client = new FakeChatClient();

    @Test
    void streamsChunksThatReconstructTheAnalysis() {
        List<String> chunks = new ArrayList<>();
        client.streamCompletion("system", "user", chunks::add);

        assertThat(chunks).isNotEmpty();
        String full = String.join("", chunks);
        assertThat(full).contains("Overall fit").contains("Suggestions");
    }
}
