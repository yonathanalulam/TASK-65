package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AudioAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioAssetRepository extends JpaRepository<AudioAsset, Long> {

    Page<AudioAsset> findByActiveTrue(Pageable pageable);

    Page<AudioAsset> findByActiveTrueAndTitleContainingIgnoreCase(String title, Pageable pageable);
}
