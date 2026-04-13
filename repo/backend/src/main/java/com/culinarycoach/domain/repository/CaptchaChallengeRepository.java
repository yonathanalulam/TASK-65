package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.CaptchaChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface CaptchaChallengeRepository extends JpaRepository<CaptchaChallenge, String> {

    @Modifying
    @Query("DELETE FROM CaptchaChallenge c WHERE c.expiresAt < :now")
    int deleteExpiredChallenges(@Param("now") Instant now);
}
