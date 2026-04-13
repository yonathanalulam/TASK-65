package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.success = false AND la.attemptedAt > :since")
    long countRecentFailures(
        @Param("username") String username,
        @Param("since") Instant since
    );

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.success = true AND la.attemptedAt > :since")
    long countRecentSuccesses(
        @Param("username") String username,
        @Param("since") Instant since
    );

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ip " +
           "AND la.success = false AND la.attemptedAt > :since")
    long countRecentFailuresByIp(
        @Param("ip") String ip,
        @Param("since") Instant since
    );

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.ipAddress = :ip AND la.success = false AND la.attemptedAt > :since")
    long countRecentFailuresByUsernameAndIp(
        @Param("username") String username,
        @Param("ip") String ip,
        @Param("since") Instant since
    );
}
