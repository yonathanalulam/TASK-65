package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.ReconciliationExport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReconciliationExportRepository extends JpaRepository<ReconciliationExport, Long> {

    List<ReconciliationExport> findByBusinessDate(LocalDate businessDate);

    List<ReconciliationExport> findByBusinessDateOrderByExportVersionDesc(LocalDate businessDate);

    @Query("SELECT COALESCE(MAX(r.exportVersion), 0) FROM ReconciliationExport r WHERE r.businessDate = :businessDate")
    int findMaxExportVersionByBusinessDate(@Param("businessDate") LocalDate businessDate);

    Page<ReconciliationExport> findAllByOrderByBusinessDateDesc(Pageable pageable);
}
