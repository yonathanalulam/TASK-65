package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.NonceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface NonceEntryRepository extends JpaRepository<NonceEntry, Long> {

    boolean existsByNonce(String nonce);

    @Modifying
    @Query("DELETE FROM NonceEntry n WHERE n.expiresAt < :now")
    int deleteExpiredNonces(@Param("now") Instant now);
}
