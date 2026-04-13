package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {

    List<AuthSession> findByUserIdAndStatusIn(Long userId, List<SessionStatus> statuses);

    long countByUserIdAndStatusIn(Long userId, List<SessionStatus> statuses);

    long countByUserIdAndDeviceIdAndStatusIn(Long userId, Long deviceId, List<SessionStatus> statuses);

    @Query("SELECT s FROM AuthSession s WHERE s.userId = :userId AND s.deviceId = :deviceId " +
           "AND s.status IN :statuses ORDER BY s.createdAt ASC")
    List<AuthSession> findOldestByUserAndDevice(
        @Param("userId") Long userId,
        @Param("deviceId") Long deviceId,
        @Param("statuses") List<SessionStatus> statuses
    );

    @Query("SELECT s FROM AuthSession s WHERE s.userId = :userId " +
           "AND s.status IN :statuses ORDER BY s.createdAt ASC")
    List<AuthSession> findOldestByUser(
        @Param("userId") Long userId,
        @Param("statuses") List<SessionStatus> statuses
    );

    @Query("SELECT s FROM AuthSession s WHERE s.status IN :statuses " +
           "AND (s.lastAccessedAt < :idleThreshold OR s.createdAt < :absoluteThreshold)")
    List<AuthSession> findExpiredSessions(
        @Param("statuses") List<SessionStatus> statuses,
        @Param("idleThreshold") Instant idleThreshold,
        @Param("absoluteThreshold") Instant absoluteThreshold
    );

    @Modifying
    @Query("UPDATE AuthSession s SET s.status = :newStatus WHERE s.id = :id")
    void updateStatus(@Param("id") String id, @Param("newStatus") SessionStatus newStatus);
}
