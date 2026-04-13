package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.JobRun;
import com.culinarycoach.domain.enums.JobRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JobRunRepository extends JpaRepository<JobRun, Long> {

    Page<JobRun> findByJobIdOrderByStartedAtDesc(Long jobId, Pageable pageable);

    List<JobRun> findByJobIdAndStatus(Long jobId, JobRunStatus status);

    @Query("SELECT COUNT(jr) FROM JobRun jr WHERE jr.jobId = :jobId " +
           "AND jr.status = :status AND jr.createdAt > :since")
    long countByJobIdAndStatusAndCreatedAtAfter(
        @Param("jobId") Long jobId,
        @Param("status") JobRunStatus status,
        @Param("since") Instant since
    );

    @Query("SELECT jr FROM JobRun jr WHERE jr.jobId = :jobId ORDER BY jr.startedAt DESC")
    List<JobRun> findLatestByJobId(@Param("jobId") Long jobId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM JobRun jr WHERE jr.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
