package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.DrillRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrillRunRepository extends JpaRepository<DrillRun, Long> {

    List<DrillRun> findByUserIdOrderByStartedAtDesc(Long userId);
}
