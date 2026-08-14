package com.jobmatch.analysis;

import com.jobmatch.chat.ChatClient;
import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.job.Job;
import com.jobmatch.job.JobRepository;
import com.jobmatch.resume.Resume;
import com.jobmatch.resume.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

@Service
public class AnalysisService {

    private static final String SYSTEM = """
            You are an expert technical career coach. You compare a candidate's résumé against a
            specific job posting and give honest, specific, actionable feedback. Be concise and use
            short bullet points. Do not invent skills the résumé does not mention.""";

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final ChatClient chatClient;

    public AnalysisService(ResumeRepository resumeRepository,
                           JobRepository jobRepository,
                           ChatClient chatClient) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.chatClient = chatClient;
    }

    /** Loads + validates the résumé (ownership) and job, and builds the prompt. Throws 404 if either is missing. */
    @Transactional(readOnly = true)
    public Prompt prepare(UUID userId, UUID resumeId, UUID jobId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + resumeId));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        String resumeText = resume.getExtractedText() == null ? "" : resume.getExtractedText();
        String company = job.getCompany() == null ? "" : " at " + job.getCompany();
        String user = "RÉSUMÉ:\n" + resumeText
                + "\n\n---\n\nJOB: " + job.getTitle() + company + "\n" + job.getDescription()
                + "\n\n---\n\nAnalyze the fit. Respond in this structure:\n"
                + "1. Overall fit (one sentence).\n"
                + "2. Matching strengths (bullets).\n"
                + "3. Gaps / missing skills not shown in the résumé (bullets).\n"
                + "4. Two or three concrete suggestions to tailor the résumé for this job.";
        return new Prompt(SYSTEM, user);
    }

    /** Streams the analysis, forwarding each text chunk to {@code onDelta}. */
    public void stream(Prompt prompt, Consumer<String> onDelta) {
        chatClient.streamCompletion(prompt.system(), prompt.user(), onDelta);
    }

    public record Prompt(String system, String user) {
    }
}
