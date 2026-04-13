package com.culinarycoach.service;

import com.culinarycoach.domain.entity.MockReceipt;
import com.culinarycoach.domain.repository.MockReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MockReceiptRepository mockReceiptRepository;

    public ReceiptService(MockReceiptRepository mockReceiptRepository) {
        this.mockReceiptRepository = mockReceiptRepository;
    }

    @Transactional
    public MockReceipt generateReceipt(Long transactionId) {
        // Check if receipt already exists for this transaction
        return mockReceiptRepository.findByTransactionId(transactionId)
                .orElseGet(() -> {
                    String receiptNumber = generateReceiptNumber();
                    MockReceipt receipt = new MockReceipt();
                    receipt.setTransactionId(transactionId);
                    receipt.setReceiptNumber(receiptNumber);
                    return mockReceiptRepository.save(receipt);
                });
    }

    public MockReceipt getReceipt(String receiptNumber) {
        return mockReceiptRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Receipt not found: " + receiptNumber));
    }

    private String generateReceiptNumber() {
        String todayStr = LocalDate.now().format(DATE_FMT);
        String prefix = "RCPT-" + todayStr + "-";

        // Get current count of receipts with today's prefix to determine sequence
        long currentCount = mockReceiptRepository.countByReceiptNumberPrefix("RCPT-" + todayStr);
        long nextSeq = currentCount + 1;

        String receiptNumber = prefix + String.format("%06d", nextSeq);

        // Ensure uniqueness - if collision, increment
        while (mockReceiptRepository.findByReceiptNumber(receiptNumber).isPresent()) {
            nextSeq++;
            receiptNumber = prefix + String.format("%06d", nextSeq);
        }

        return receiptNumber;
    }
}
