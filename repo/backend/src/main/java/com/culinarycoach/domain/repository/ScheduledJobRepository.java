package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {

    Optional<ScheduledJob> findByJobName(String jobName);

    List<ScheduledJob> findByEnabledTrue();
}
