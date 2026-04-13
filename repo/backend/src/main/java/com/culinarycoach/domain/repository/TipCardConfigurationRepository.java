package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.TipCardConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipCardConfigurationRepository extends JpaRepository<TipCardConfiguration, Long> {

    Optional<TipCardConfiguration> findByScopeAndScopeId(String scope, Long scopeId);
}
