package com.culinarycoach.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reconciliation_exports",
       uniqueConstraints = @UniqueConstraint(columnNames = {"business_date", "export_version"}))
public class ReconciliationExport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "export_version", nullable = false)
    private int exportVersion = 1;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_checksum", nullable = false, length = 64)
    private String fileChecksum;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount = 0;

    @Column(name = "total_completed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCompletedAmount = BigDecimal.ZERO;

    @Column(name = "total_voided_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVoidedAmount = BigDecimal.ZERO;

    @Column(name = "generated_by", nullable = false, length = 64)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.generatedAt == null) {
            this.generatedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }

    public int getExportVersion() { return exportVersion; }
    public void setExportVersion(int exportVersion) { this.exportVersion = exportVersion; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileChecksum() { return fileChecksum; }
    public void setFileChecksum(String fileChecksum) { this.fileChecksum = fileChecksum; }

    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

    public BigDecimal getTotalCompletedAmount() { return totalCompletedAmount; }
    public void setTotalCompletedAmount(BigDecimal totalCompletedAmount) { this.totalCompletedAmount = totalCompletedAmount; }

    public BigDecimal getTotalVoidedAmount() { return totalVoidedAmount; }
    public void setTotalVoidedAmount(BigDecimal totalVoidedAmount) { this.totalVoidedAmount = totalVoidedAmount; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
