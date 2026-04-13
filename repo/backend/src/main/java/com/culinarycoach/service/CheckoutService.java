package com.culinarycoach.service;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.audit.TraceContext;
import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.*;
import com.culinarycoach.domain.enums.TransactionStatus;
import com.culinarycoach.domain.repository.*;
import com.culinarycoach.security.mfa.EncryptionUtil;
import com.culinarycoach.web.dto.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final ProductBundleRepository productBundleRepository;
    private final MockTransactionRepository mockTransactionRepository;
    private final MockTransactionItemRepository mockTransactionItemRepository;
    private final MockReceiptRepository mockReceiptRepository;
    private final AudioBundleEntitlementRepository audioBundleEntitlementRepository;
    private final ReceiptService receiptService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    public CheckoutService(ProductBundleRepository productBundleRepository,
                           MockTransactionRepository mockTransactionRepository,
                           MockTransactionItemRepository mockTransactionItemRepository,
                           MockReceiptRepository mockReceiptRepository,
                           AudioBundleEntitlementRepository audioBundleEntitlementRepository,
                           ReceiptService receiptService,
                           AuditService auditService,
                           AppProperties appProperties) {
        this.productBundleRepository = productBundleRepository;
        this.mockTransactionRepository = mockTransactionRepository;
        this.mockTransactionItemRepository = mockTransactionItemRepository;
        this.mockReceiptRepository = mockReceiptRepository;
        this.audioBundleEntitlementRepository = audioBundleEntitlementRepository;
        this.receiptService = receiptService;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }

    public List<ProductBundleResponse> listBundles() {
        return productBundleRepository.findByActiveTrue().stream()
                .map(b -> new ProductBundleResponse(b.getId(), b.getName(), b.getDescription(), b.getPrice()))
                .toList();
    }

    @Transactional
    public TransactionResponse initiateCheckout(Long userId, List<Long> bundleIds) {
        if (bundleIds == null || bundleIds.isEmpty()) {
            throw new IllegalArgumentException("At least one bundle ID is required");
        }

        List<ProductBundle> bundles = new ArrayList<>();
        for (Long bundleId : bundleIds) {
            ProductBundle bundle = productBundleRepository.findById(bundleId)
                    .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId));
            if (!bundle.isActive()) {
                throw new IllegalArgumentException("Bundle is not active: " + bundle.getName());
            }
            bundles.add(bundle);
        }

        // Generate and encrypt payment reference token
        String rawToken = UUID.randomUUID().toString();
        String encryptionKey = appProperties.getSecurity().getMfa().getEncryptionKey();
        String encryptedToken = EncryptionUtil.encrypt(rawToken, encryptionKey);

        // Calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ProductBundle bundle : bundles) {
            totalAmount = totalAmount.add(bundle.getPrice());
        }

        // Create transaction
        MockTransaction transaction = new MockTransaction();
        transaction.setUserId(userId);
        transaction.setStatus(TransactionStatus.INITIATED);
        transaction.setTotalAmount(totalAmount);
        transaction.setPaymentReferenceToken(encryptedToken);
        transaction.setTraceId(TraceContext.get());
        transaction = mockTransactionRepository.save(transaction);

        // Create line items
        List<MockTransactionItem> items = new ArrayList<>();
        for (ProductBundle bundle : bundles) {
            MockTransactionItem item = new MockTransactionItem();
            item.setTransactionId(transaction.getId());
            item.setBundleId(bundle.getId());
            item.setBundleName(bundle.getName());
            item.setUnitPrice(bundle.getPrice());
            item.setQuantity(1);
            item.setLineTotal(bundle.getPrice());
            items.add(mockTransactionItemRepository.save(item));
        }

        return toTransactionResponse(transaction, items, null, rawToken);
    }

    @Transactional
    public TransactionResponse completeCheckout(Long transactionId, Long userId) {
        MockTransaction transaction = mockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new IllegalStateException(
                    "Transaction cannot be completed; current status: " + transaction.getStatus());
        }

        if (!transaction.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Transaction does not belong to user");
        }

        // Transition to COMPLETED
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(Instant.now());
        transaction = mockTransactionRepository.save(transaction);

        // Generate receipt
        MockReceipt receipt = receiptService.generateReceipt(transactionId);

        // Grant entitlements
        List<MockTransactionItem> items = mockTransactionItemRepository.findByTransactionId(transactionId);
        for (MockTransactionItem item : items) {
            if (!audioBundleEntitlementRepository.existsByUserIdAndBundleIdAndRevokedAtIsNull(
                    userId, item.getBundleId())) {
                AudioBundleEntitlement entitlement = new AudioBundleEntitlement();
                entitlement.setUserId(userId);
                entitlement.setBundleId(item.getBundleId());
                entitlement.setSource("MOCK_CHECKOUT");
                entitlement.setGrantedBy("system");
                audioBundleEntitlementRepository.save(entitlement);
            }
        }

        // Audit log
        auditService.log(AuditEventType.USER_CREATED, userId, null,
                null, null, "MOCK_TRANSACTION", transactionId.toString(),
                "Checkout completed. Receipt: " + receipt.getReceiptNumber()
                        + ", Amount: " + transaction.getTotalAmount());

        return toTransactionResponse(transaction, items, receipt, null);
    }

    @Transactional
    public void failCheckout(Long transactionId) {
        MockTransaction transaction = mockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new IllegalStateException(
                    "Transaction cannot be failed; current status: " + transaction.getStatus());
        }

        transaction.setStatus(TransactionStatus.FAILED);
        mockTransactionRepository.save(transaction);

        log.info("Transaction {} marked as FAILED", transactionId);
    }

    @Transactional
    public TransactionResponse voidTransaction(Long transactionId, String adminUsername, String reason) {
        MockTransaction transaction = mockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only COMPLETED transactions can be voided; current status: " + transaction.getStatus());
        }

        // Compensating void - original COMPLETED record stays, we mark it as VOIDED
        transaction.setStatus(TransactionStatus.VOIDED);
        transaction.setVoidedAt(Instant.now());
        transaction.setVoidedBy(adminUsername);
        transaction.setVoidReason(reason);
        transaction = mockTransactionRepository.save(transaction);

        // Revoke entitlements
        List<MockTransactionItem> items = mockTransactionItemRepository.findByTransactionId(transactionId);
        for (MockTransactionItem item : items) {
            List<AudioBundleEntitlement> entitlements =
                    audioBundleEntitlementRepository.findByUserId(transaction.getUserId());
            for (AudioBundleEntitlement ent : entitlements) {
                if (ent.getBundleId().equals(item.getBundleId()) && ent.getRevokedAt() == null) {
                    ent.setRevokedAt(Instant.now());
                    audioBundleEntitlementRepository.save(ent);
                }
            }
        }

        // Compensating audit entry
        auditService.log(AuditEventType.USER_STATUS_CHANGE, transaction.getUserId(), adminUsername,
                null, null, "MOCK_TRANSACTION", transactionId.toString(),
                "Transaction VOIDED by " + adminUsername + ". Reason: " + reason
                        + ". Amount: " + transaction.getTotalAmount());

        MockReceipt receipt = mockReceiptRepository.findByTransactionId(transactionId).orElse(null);
        return toTransactionResponse(transaction, items, receipt, null);
    }

    public TransactionResponse getTransaction(Long transactionId, Long actorUserId) {
        MockTransaction transaction = mockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (!transaction.getUserId().equals(actorUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Transaction does not belong to the authenticated user");
        }

        List<MockTransactionItem> items = mockTransactionItemRepository.findByTransactionId(transactionId);
        MockReceipt receipt = mockReceiptRepository.findByTransactionId(transactionId).orElse(null);

        return toTransactionResponse(transaction, items, receipt, null);
    }

    public Page<TransactionResponse> listUserTransactions(Long userId, Pageable pageable) {
        return mockTransactionRepository.findByUserId(userId, pageable)
                .map(txn -> {
                    List<MockTransactionItem> items =
                            mockTransactionItemRepository.findByTransactionId(txn.getId());
                    MockReceipt receipt =
                            mockReceiptRepository.findByTransactionId(txn.getId()).orElse(null);
                    return toTransactionResponse(txn, items, receipt, null);
                });
    }

    private TransactionResponse toTransactionResponse(MockTransaction txn,
                                                       List<MockTransactionItem> items,
                                                       MockReceipt receipt,
                                                       String rawToken) {
        List<TransactionItemResponse> itemResponses = items.stream()
                .map(i -> new TransactionItemResponse(
                        i.getBundleId(), i.getBundleName(),
                        i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .toList();

        String receiptNumber = receipt != null ? receipt.getReceiptNumber() : null;
        String paymentRefMasked = maskPaymentRef(txn.getPaymentReferenceToken(), rawToken);

        return new TransactionResponse(
                txn.getId(),
                txn.getStatus().name(),
                txn.getTotalAmount(),
                receiptNumber,
                txn.getInitiatedAt(),
                txn.getCompletedAt(),
                itemResponses,
                paymentRefMasked
        );
    }

    private String maskPaymentRef(String encryptedToken, String rawToken) {
        // If we have the raw token from initiation, mask it
        if (rawToken != null && rawToken.length() >= 4) {
            return "****" + rawToken.substring(rawToken.length() - 4);
        }

        // Otherwise, decrypt and mask
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
        return null;
    }
}
