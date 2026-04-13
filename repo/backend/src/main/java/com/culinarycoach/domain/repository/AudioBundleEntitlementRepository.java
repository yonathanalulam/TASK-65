package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AudioBundleEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioBundleEntitlementRepository extends JpaRepository<AudioBundleEntitlement, Long> {

    List<AudioBundleEntitlement> findByUserId(Long userId);

    boolean existsByUserIdAndBundleIdAndRevokedAtIsNull(Long userId, Long bundleId);
}
