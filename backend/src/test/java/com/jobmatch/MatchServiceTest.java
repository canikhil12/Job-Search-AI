package com.jobmatch;

import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.embedding.EmbeddingClient;
import com.jobmatch.job.JobVectorRepository;
import com.jobmatch.job.dto.JobMatchResponse;
import com.jobmatch.match.MatchService;
import com.jobmatch.resume.Resume;
import com.jobmatch.resume.ResumeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;
    @Mock
    private JobVectorRepository jobVectorRepository;
    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private MatchService matchService;

    @Test
    void matchText_embedsAndSearchesWithDefaultLimit() {
        float[] vec = new float[1536];
        when(embeddingClient.embed("java spring")).thenReturn(vec);
        when(jobVectorRepository.search(vec, 5)).thenReturn(List.of());

        matchService.matchText("java spring", null);

        verify(jobVectorRepository).search(vec, 5); // null -> default 5
    }

    @Test
    void matchText_clampsLimitToMax() {
        when(embeddingClient.embed(any())).thenReturn(new float[1536]);
        matchService.matchText("x", 100);
        verify(jobVectorRepository).search(any(), eq(20)); // clamped to 20
    }

    @Test
    void matchResume_embedsResumeTextAndSearches() {
        UUID userId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        Resume resume = new Resume(resumeId, userId, "cv.pdf", "application/pdf", 10,
                "key", "Senior Java engineer, Spring Boot", null);
        when(resumeRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(resume));
        float[] vec = new float[1536];
        when(embeddingClient.embed("Senior Java engineer, Spring Boot")).thenReturn(vec);
        when(jobVectorRepository.search(vec, 5)).thenReturn(List.of(
                new JobMatchResponse(UUID.randomUUID(), "Backend Engineer", "Acme", "Remote", null, 0.8)));

        List<JobMatchResponse> matches = matchService.matchResume(userId, resumeId, null);

        assertThat(matches).hasSize(1);
        verify(embeddingClient).embed("Senior Java engineer, Spring Boot");
    }

    @Test
    void matchResume_notOwned_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        when(resumeRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.matchResume(userId, resumeId, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void matchResume_blankText_returnsEmptyWithoutSearching() {
        UUID userId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        Resume resume = new Resume(resumeId, userId, "cv.pdf", "application/pdf", 10, "key", "  ", null);
        when(resumeRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(resume));

        assertThat(matchService.matchResume(userId, resumeId, null)).isEmpty();
        verify(jobVectorRepository, never()).search(any(), anyInt());
    }
}
