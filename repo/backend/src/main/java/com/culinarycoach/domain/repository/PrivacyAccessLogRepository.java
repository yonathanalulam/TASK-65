package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.PrivacyAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivacyAccessLogRepository extends JpaRepository<PrivacyAccessLog, Long> {

    Page<PrivacyAccessLog> findBySubjectUserId(Long subjectUserId, Pageable pageable);

    Page<PrivacyAccessLog> findByViewerUserId(Long viewerUserId, Pageable pageable);
}
