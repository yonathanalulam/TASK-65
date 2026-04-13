package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.StepTipBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StepTipBindingRepository extends JpaRepository<StepTipBinding, Long> {

    List<StepTipBinding> findByStepId(Long stepId);
}
