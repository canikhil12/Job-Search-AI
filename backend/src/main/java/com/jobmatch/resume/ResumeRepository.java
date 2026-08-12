package com.jobmatch.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Ownership-scoped lookup: only returns the row if it belongs to the given user.
    Optional<Resume> findByIdAndUserId(UUID id, UUID userId);
}
