package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.TipCardAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipCardAuditLogRepository extends JpaRepository<TipCardAuditLog, Long> {
}
