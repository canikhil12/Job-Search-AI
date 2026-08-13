package com.jobmatch;

import com.jobmatch.embedding.FakeEmbeddingClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FakeEmbeddingClientTest {

    private final FakeEmbeddingClient client = new FakeEmbeddingClient(1536);

    @Test
    void producesVectorOfConfiguredDimension() {
        assertThat(client.embed("hello").length).isEqualTo(1536);
        assertThat(client.dimension()).isEqualTo(1536);
    }

    @Test
    void isDeterministicForSameText() {
        assertThat(client.embed("Java backend engineer"))
                .containsExactly(client.embed("Java backend engineer"));
    }

    @Test
    void differsForDifferentText() {
        assertThat(client.embed("data engineer"))
                .isNotEqualTo(client.embed("frontend designer"));
    }

    @Test
    void isL2Normalized() {
        float[] v = client.embed("some resume text");
        double sumSquares = 0;
        for (float f : v) {
            sumSquares += (double) f * f;
        }
        assertThat(Math.sqrt(sumSquares)).isCloseTo(1.0, within(1e-4));
    }
}
