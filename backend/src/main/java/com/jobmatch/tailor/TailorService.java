package com.jobmatch.tailor;

import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.job.Job;
import com.jobmatch.job.JobRepository;
import com.jobmatch.resume.Resume;
import com.jobmatch.resume.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TailorService {

    private static final String SYSTEM = """
            You are an expert résumé writer. Rewrite and improve the candidate's résumé to better
            target this specific job. Use ONLY real experience from their résumé — never fabricate
            skills or experience. Surface and emphasize the keywords/skills the job asks for that the
            résumé genuinely supports. Output concrete, copy-pasteable improvements: a tightened
            professional summary, then improved bullet points grouped by section. Be honest and specific.""";

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;

    public TailorService(ResumeRepository resumeRepository, JobRepository jobRepository) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public Prompt prepare(UUID userId, UUID resumeId, UUID jobId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + resumeId));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        String resumeText = resume.getExtractedText() == null ? "" : resume.getExtractedText();
        String company = job.getCompany() == null ? "" : " at " + job.getCompany();
        String user = "RÉSUMÉ:\n" + resumeText
                + "\n\n---\n\nTARGET JOB: " + job.getTitle() + company + "\n" + job.getDescription()
                + "\n\n---\n\nRewrite the résumé to target this job now.";
        return new Prompt(SYSTEM, user);
    }

    public record Prompt(String system, String user) {
    }
}
