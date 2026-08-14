package com.jobmatch.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Read-side repository for job metadata (JPA). Vector writes/searches live in JobVectorRepository. */
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByOrderByCreatedAtDesc();

    boolean existsByExternalId(String externalId);

    List<Job> findByExternalIdInOrderByPostedAtDesc(Collection<String> externalIds);
}
