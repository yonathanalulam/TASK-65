package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.MockReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MockReceiptRepository extends JpaRepository<MockReceipt, Long> {

    Optional<MockReceipt> findByTransactionId(Long transactionId);

    Optional<MockReceipt> findByReceiptNumber(String receiptNumber);

    @Query("SELECT COALESCE(MAX(r.id), 0) FROM MockReceipt r WHERE r.receiptNumber LIKE :prefix%")
    long countByReceiptNumberPrefix(@Param("prefix") String prefix);
}
