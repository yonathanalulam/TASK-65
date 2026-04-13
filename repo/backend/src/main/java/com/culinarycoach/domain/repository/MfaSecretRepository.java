package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.MfaSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MfaSecretRepository extends JpaRepository<MfaSecret, Long> {

    Optional<MfaSecret> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
