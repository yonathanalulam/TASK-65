package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AnomalyAlert;
import com.culinarycoach.domain.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlert, Long> {

    Page<AnomalyAlert> findByStatus(AlertStatus status, Pageable pageable);

    List<AnomalyAlert> findByStatusIn(Collection<AlertStatus> statuses);

    long countByStatus(AlertStatus status);
}
