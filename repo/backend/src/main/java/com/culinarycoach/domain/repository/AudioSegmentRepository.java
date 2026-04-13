package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AudioSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioSegmentRepository extends JpaRepository<AudioSegment, Long> {

    List<AudioSegment> findByAudioAssetId(Long audioAssetId);
}
