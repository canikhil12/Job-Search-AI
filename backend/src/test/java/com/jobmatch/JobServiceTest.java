package com.jobmatch;

import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.embedding.EmbeddingClient;
import com.jobmatch.job.Job;
import com.jobmatch.job.JobRepository;
import com.jobmatch.job.JobService;
import com.jobmatch.job.JobVectorRepository;
import com.jobmatch.job.dto.CreateJobRequest;
import com.jobmatch.job.dto.JobResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobVectorRepository jobVectorRepository;
    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private JobService jobService;

    @Test
    void ingest_embedsTextAndStoresWithVector() {
        float[] vector = new float[1536];
        when(embeddingClient.embed(any())).thenReturn(vector);
        CreateJobRequest req = new CreateJobRequest(
                "Senior Backend Engineer", "flexEngage", "Remote",
                "Java, Spring Boot, AWS, Kubernetes", "https://example.com/apply");

        JobResponse response = jobService.ingest(req, "manual");

        assertThat(response.title()).isEqualTo("Senior Backend Engineer");
        assertThat(response.source()).isEqualTo("manual");

        // the embedded text includes title, company, location and description
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingClient).embed(textCaptor.capture());
        assertThat(textCaptor.getValue())
                .contains("Senior Backend Engineer")
                .contains("flexEngage")
                .contains("Spring Boot");

        verify(jobVectorRepository).insert(any(Job.class), any(float[].class));
    }

    @Test
    void get_missing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.get(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
