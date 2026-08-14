package com.jobmatch.coverletter;

import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.job.Job;
import com.jobmatch.job.JobRepository;
import com.jobmatch.resume.Resume;
import com.jobmatch.resume.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CoverLetterService {

    private static final String SYSTEM = """
            You are an expert career writer. Write a concise, compelling cover letter (about 250-300
            words) tailored to this specific job, drawing on concrete details from the candidate's
            résumé that match the role. Professional and warm, no clichés, and never invent experience
            the résumé does not contain. Output only the letter body — no address blocks or bracketed
            placeholders.""";

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;

    public CoverLetterService(ResumeRepository resumeRepository, JobRepository jobRepository) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
    }

    /** Loads + validates the résumé (ownership) and job, and builds the prompt. Throws 404 if missing. */
    @Transactional(readOnly = true)
    public Prompt prepare(UUID userId, UUID resumeId, UUID jobId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + resumeId));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        String resumeText = resume.getExtractedText() == null ? "" : resume.getExtractedText();
        String company = job.getCompany() == null ? "the company" : job.getCompany();
        String user = "RÉSUMÉ:\n" + resumeText
                + "\n\n---\n\nJOB: " + job.getTitle() + " at " + company + "\n" + job.getDescription()
                + "\n\n---\n\nWrite the tailored cover letter now.";
        return new Prompt(SYSTEM, user);
    }

    public record Prompt(String system, String user) {
    }
}
