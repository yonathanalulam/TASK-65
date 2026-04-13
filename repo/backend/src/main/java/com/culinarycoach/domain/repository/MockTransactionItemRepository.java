package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.MockTransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockTransactionItemRepository extends JpaRepository<MockTransactionItem, Long> {

    List<MockTransactionItem> findByTransactionId(Long transactionId);
}
