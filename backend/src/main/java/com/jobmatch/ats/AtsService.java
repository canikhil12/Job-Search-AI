package com.jobmatch.ats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatch.chat.ChatClient;
import com.jobmatch.chat.ChatException;
import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.job.Job;
import com.jobmatch.job.JobRepository;
import com.jobmatch.resume.Resume;
import com.jobmatch.resume.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AtsService {

    private static final String SYSTEM = """
            You are an ATS (Applicant Tracking System) analyzer. Compare the résumé to the job and
            respond with a JSON object ONLY — no prose, no markdown fences — with exactly these keys:
            "score" (integer 0-100: how well the résumé matches the job's key requirements/keywords),
            "matchedKeywords" (array of the important skills/keywords from the job that ARE present in
            the résumé), "missingKeywords" (array of important job skills/keywords NOT in the résumé),
            "summary" (one short sentence). Base keywords on the job's real requirements; do not invent.""";

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AtsService(ResumeRepository resumeRepository,
                      JobRepository jobRepository,
                      ChatClient chatClient,
                      ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AtsResult score(UUID userId, UUID resumeId, UUID jobId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + resumeId));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        String resumeText = resume.getExtractedText() == null ? "" : resume.getExtractedText();
        String company = job.getCompany() == null ? "" : " at " + job.getCompany();
        String user = "RÉSUMÉ:\n" + resumeText
                + "\n\n---\n\nJOB: " + job.getTitle() + company + "\n" + job.getDescription();

        return parse(chatClient.complete(SYSTEM, user));
    }

    private AtsResult parse(String raw) {
        int start = raw == null ? -1 : raw.indexOf('{');
        int end = raw == null ? -1 : raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ChatException("ATS response was not JSON");
        }
        try {
            JsonNode n = objectMapper.readTree(raw.substring(start, end + 1));
            int score = Math.max(0, Math.min(100, n.path("score").asInt()));
            return new AtsResult(score, toList(n.path("matchedKeywords")),
                    toList(n.path("missingKeywords")), n.path("summary").asText(""));
        } catch (Exception ex) {
            throw new ChatException("Could not parse ATS response", ex);
        }
    }

    private List<String> toList(JsonNode array) {
        List<String> out = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(item -> out.add(item.asText()));
        }
        return out;
    }
}
