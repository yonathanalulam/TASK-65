package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.ParentCoachAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentCoachAssignmentRepository extends JpaRepository<ParentCoachAssignment, Long> {

    List<ParentCoachAssignment> findByCoachUserIdAndRevokedAtIsNull(Long coachUserId);

    boolean existsByCoachUserIdAndStudentUserIdAndRevokedAtIsNull(Long coachUserId, Long studentUserId);

    Optional<ParentCoachAssignment> findByCoachUserIdAndStudentUserId(Long coachUserId, Long studentUserId);
}
