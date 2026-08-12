package com.jobmatch.resume.parse;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Extracts plain text from a resume file using Apache Tika, which auto-detects the format
 * (PDF, DOCX, …) and delegates to the right parser. The extracted text is what later phases
 * embed for semantic matching.
 */
@Component
public class ResumeTextExtractor {

    // Resumes are short; 1M chars is far more than enough and guards against pathological files.
    private static final int MAX_CHARS = 1_000_000;

    private final Tika tika;

    public ResumeTextExtractor() {
        this.tika = new Tika();
        this.tika.setMaxStringLength(MAX_CHARS);
    }

    /** Returns the extracted text, trimmed. Throws {@link ResumeParseException} on unreadable files. */
    public String extract(byte[] content) {
        try (InputStream in = new ByteArrayInputStream(content)) {
            String text = tika.parseToString(in);
            return text == null ? "" : text.strip();
        } catch (Exception ex) {
            // Tika throws TikaException/IOException for corrupt or unreadable documents.
            throw new ResumeParseException("Could not extract text from the uploaded file", ex);
        }
    }
}
