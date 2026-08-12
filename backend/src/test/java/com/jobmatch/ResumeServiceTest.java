package com.jobmatch;

import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.resume.Resume;
import com.jobmatch.resume.ResumeRepository;
import com.jobmatch.resume.ResumeService;
import com.jobmatch.resume.UnsupportedFileTypeException;
import com.jobmatch.resume.dto.ResumeDetailResponse;
import com.jobmatch.resume.parse.ResumeTextExtractor;
import com.jobmatch.resume.storage.ResumeStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    private static final String PDF = "application/pdf";

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ResumeStorage storage;
    @Mock
    private ResumeTextExtractor textExtractor;

    @InjectMocks
    private ResumeService resumeService;

    @Test
    void upload_happyPath_storesFileExtractsTextAndPersists() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", PDF, "pdf-bytes".getBytes());
        when(textExtractor.extract(any())).thenReturn("Jane Dev — Senior Engineer");

        ResumeDetailResponse response = resumeService.upload(userId, file);

        assertThat(response.fileName()).isEqualTo("cv.pdf");
        assertThat(response.contentType()).isEqualTo(PDF);
        assertThat(response.extractedText()).isEqualTo("Jane Dev — Senior Engineer");

        // stored under "<userId>/<resumeId>.pdf"
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storage).upload(keyCaptor.capture(), eq(PDF), any());
        assertThat(keyCaptor.getValue()).startsWith(userId.toString() + "/").endsWith(".pdf");
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    void upload_unsupportedType_rejectedAndNothingStored() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> resumeService.upload(userId, file))
                .isInstanceOf(UnsupportedFileTypeException.class);

        verify(storage, never()).upload(any(), any(), any());
        verify(resumeRepository, never()).save(any());
    }

    @Test
    void upload_emptyFile_rejected() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", PDF, new byte[0]);

        assertThatThrownBy(() -> resumeService.upload(userId, file))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void get_notOwned_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        when(resumeRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.get(userId, resumeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesRowThenObject() {
        UUID userId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();
        Resume resume = new Resume(resumeId, userId, "cv.pdf", PDF, 10,
                userId + "/" + resumeId + ".pdf", "text", null);
        when(resumeRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(resume));

        resumeService.delete(userId, resumeId);

        verify(resumeRepository).delete(resume);
        verify(storage).delete(resume.getStorageKey());
    }
}
