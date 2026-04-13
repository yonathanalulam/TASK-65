package com.culinarycoach.service;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.*;
import com.culinarycoach.domain.enums.TransactionStatus;
import com.culinarycoach.domain.repository.*;
import com.culinarycoach.security.mfa.EncryptionUtil;
import com.culinarycoach.web.dto.response.ReconciliationExportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MockTransactionRepository mockTransactionRepository;
    private final MockTransactionItemRepository mockTransactionItemRepository;
    private final MockReceiptRepository mockReceiptRepository;
    private final ReconciliationExportRepository reconciliationExportRepository;
    private final AuditService auditService;
    private final AppProperties appProperties;

    public ReconciliationService(MockTransactionRepository mockTransactionRepository,
                                  MockTransactionItemRepository mockTransactionItemRepository,
                                  MockReceiptRepository mockReceiptRepository,
                                  ReconciliationExportRepository reconciliationExportRepository,
                                  AuditService auditService,
                                  AppProperties appProperties) {
        this.mockTransactionRepository = mockTransactionRepository;
        this.mockTransactionItemRepository = mockTransactionItemRepository;
        this.mockReceiptRepository = mockReceiptRepository;
        this.reconciliationExportRepository = reconciliationExportRepository;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }

    @Transactional
    public ReconciliationExportResponse generateExport(LocalDate businessDate, String adminUsername) {
        // Determine next version
        int maxVersion = reconciliationExportRepository.findMaxExportVersionByBusinessDate(businessDate);
        int nextVersion = maxVersion + 1;

        // Gather COMPLETED transactions for the date
        Instant dayStart = businessDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = businessDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<MockTransaction> completedTxns =
                mockTransactionRepository.findByStatusAndCompletedAtBetween(
                        TransactionStatus.COMPLETED, dayStart, dayEnd);

        // Gather VOIDED transactions (voided_at within the business date)
        List<MockTransaction> allVoided = mockTransactionRepository.findByStatus(TransactionStatus.VOIDED);
        List<MockTransaction> voidedTxns = allVoided.stream()
                .filter(t -> t.getVoidedAt() != null
                        && !t.getVoidedAt().isBefore(dayStart)
                        && t.getVoidedAt().isBefore(dayEnd))
                .toList();

        // Combine all transactions
        List<MockTransaction> allTxns = new ArrayList<>();
        allTxns.addAll(completedTxns);
        allTxns.addAll(voidedTxns);

        // Calculate totals
        BigDecimal totalCompleted = completedTxns.stream()
                .map(MockTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVoided = voidedTxns.stream()
                .map(MockTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("receipt_number,transaction_status,bundle_id,bundle_name,amount,payment_ref_masked,completed_at,voided_at,operator_id\n");

        for (MockTransaction txn : allTxns) {
            MockReceipt receipt = mockReceiptRepository.findByTransactionId(txn.getId()).orElse(null);
            String receiptNumber = receipt != null ? receipt.getReceiptNumber() : "";
            String maskedRef = maskPaymentRef(txn.getPaymentReferenceToken());

            List<MockTransactionItem> items = mockTransactionItemRepository.findByTransactionId(txn.getId());
            for (MockTransactionItem item : items) {
                csv.append(escapeCsv(receiptNumber)).append(",");
                csv.append(txn.getStatus().name()).append(",");
                csv.append(item.getBundleId()).append(",");
                csv.append(escapeCsv(item.getBundleName())).append(",");
                csv.append(item.getLineTotal()).append(",");
                csv.append(escapeCsv(maskedRef)).append(",");
                csv.append(txn.getCompletedAt() != null ? txn.getCompletedAt().toString() : "").append(",");
                csv.append(txn.getVoidedAt() != null ? txn.getVoidedAt().toString() : "").append(",");
                csv.append(txn.getOperatorUserId() != null ? txn.getOperatorUserId().toString() : "");
                csv.append("\n");
            }
        }

        String csvContent = csv.toString();

        // Compute SHA-256 checksum
        String checksum = computeSha256(csvContent);

        // Write CSV file
        String fileName = "reconciliation_" + businessDate.format(DATE_FMT) + "_v" + nextVersion + ".csv";
        Path exportDir = Paths.get("exports", "reconciliation");
        Path filePath;
        try {
            Files.createDirectories(exportDir);
            filePath = exportDir.resolve(fileName);
            Files.writeString(filePath, csvContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write export file: " + e.getMessage(), e);
        }

        // Store metadata
        ReconciliationExport export = new ReconciliationExport();
        export.setBusinessDate(businessDate);
        export.setExportVersion(nextVersion);
        export.setFilePath(filePath.toString());
        export.setFileChecksum(checksum);
        export.setTransactionCount(allTxns.size());
        export.setTotalCompletedAmount(totalCompleted);
        export.setTotalVoidedAmount(totalVoided);
        export.setGeneratedBy(adminUsername);
        export = reconciliationExportRepository.save(export);

        // Audit
        auditService.log(AuditEventType.USER_STATUS_CHANGE, null, adminUsername,
                null, null, "RECONCILIATION_EXPORT", export.getId().toString(),
                "Reconciliation export generated for " + businessDate
                        + " v" + nextVersion + ". Transactions: " + allTxns.size()
                        + ", Completed: " + totalCompleted + ", Voided: " + totalVoided);

        log.info("Reconciliation export generated: date={}, version={}, transactions={}, file={}",
                businessDate, nextVersion, allTxns.size(), filePath);

        return toResponse(export);
    }

    public ReconciliationExportResponse getExport(Long exportId) {
        ReconciliationExport export = reconciliationExportRepository.findById(exportId)
                .orElseThrow(() -> new IllegalArgumentException("Export not found: " + exportId));
        return toResponse(export);
    }

    public Page<ReconciliationExportResponse> listExports(Pageable pageable) {
        return reconciliationExportRepository.findAllByOrderByBusinessDateDesc(pageable)
                .map(this::toResponse);
    }

    private ReconciliationExportResponse toResponse(ReconciliationExport export) {
        return new ReconciliationExportResponse(
                export.getId(),
                export.getBusinessDate(),
                export.getExportVersion(),
                export.getFilePath(),
                export.getFileChecksum(),
                export.getTransactionCount(),
                export.getTotalCompletedAmount(),
                export.getTotalVoidedAmount(),
                export.getGeneratedBy(),
                export.getGeneratedAt()
        );
    }

    private String maskPaymentRef(String encryptedToken) {
        if (encryptedToken != null && !encryptedToken.isBlank()) {
            try {
                String encryptionKey = appProperties.getSecurity().getMfa().getEncryptionKey();
                String decrypted = EncryptionUtil.decrypt(encryptedToken, encryptionKey);
                if (decrypted.length() >= 4) {
                    return "****" + decrypted.substring(decrypted.length() - 4);
                }
                return "****";
            } catch (Exception e) {
                log.warn("Failed to decrypt payment reference for masking", e);
                return "****";
            }
        }
        return "";
    }

    private String computeSha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
