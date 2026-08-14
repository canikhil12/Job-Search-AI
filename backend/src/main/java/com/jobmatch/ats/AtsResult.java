package com.jobmatch.ats;

import java.util.List;

/** An ATS-style match: 0–100 keyword-coverage score plus which job keywords are matched/missing. */
public record AtsResult(
        int score,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        String summary
) {
}
