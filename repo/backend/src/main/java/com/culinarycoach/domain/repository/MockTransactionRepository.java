package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.MockTransaction;
import com.culinarycoach.domain.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MockTransactionRepository extends JpaRepository<MockTransaction, Long> {

    Page<MockTransaction> findByUserId(Long userId, Pageable pageable);

    List<MockTransaction> findByStatusAndCompletedAtBetween(
            TransactionStatus status, Instant from, Instant to);

    List<MockTransaction> findByStatus(TransactionStatus status);
}
