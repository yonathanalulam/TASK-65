package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.MetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, Long> {

    List<MetricSnapshot> findByMetricNameAndWindowStartBetween(
        String metricName, Instant from, Instant to);

    @Query("SELECT ms FROM MetricSnapshot ms WHERE ms.metricName = :metricName " +
           "ORDER BY ms.windowEnd DESC LIMIT 1")
    Optional<MetricSnapshot> findLatestByMetricName(@Param("metricName") String metricName);

    @Query("SELECT ms FROM MetricSnapshot ms WHERE ms.dimensionKey = :dimensionKey " +
           "AND ms.dimensionValue = :dimensionValue ORDER BY ms.windowEnd DESC LIMIT 1")
    Optional<MetricSnapshot> findLatestByDimension(
        @Param("dimensionKey") String dimensionKey,
        @Param("dimensionValue") String dimensionValue);

    @Query("SELECT ms FROM MetricSnapshot ms WHERE ms.dimensionKey = :dimensionKey " +
           "AND ms.dimensionValue = :dimensionValue ORDER BY ms.windowEnd DESC")
    List<MetricSnapshot> findByDimensionOrderByWindowEndDesc(
        @Param("dimensionKey") String dimensionKey,
        @Param("dimensionValue") String dimensionValue);

    @Modifying
    @Query("DELETE FROM MetricSnapshot ms WHERE ms.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
