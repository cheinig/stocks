package com.stockstatus.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ETFAllocation Entity - represents the allocation of a stock within an ETF
 * Tracks historical versions to show how ETF composition changes over time
 * Maps to the 'etf_allocations' table in the database
 */
@Entity
@Table(name = "etf_allocations", indexes = {
    @Index(name = "idx_etf_allocations_etf_version", columnList = "etf_id, allocation_version DESC"),
    @Index(name = "idx_etf_allocations_stock", columnList = "stock_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"etf", "stock"})
@EqualsAndHashCode(of = "id")
public class ETFAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "ETF reference is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etf_id", nullable = false, foreignKey = @ForeignKey(name = "fk_allocation_etf"))
    private ETF etf;

    @NotNull(message = "Stock reference is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false, foreignKey = @ForeignKey(name = "fk_allocation_stock"))
    private Stock stock;

    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.000001", message = "Percentage must be greater than 0")
    @DecimalMax(value = "100.0", message = "Percentage must not exceed 100")
    @Column(name = "percentage", nullable = false, precision = 10, scale = 6)
    private BigDecimal percentage;

    @CreationTimestamp
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    @NotNull(message = "Allocation version is required")
    @Column(name = "allocation_version", nullable = false)
    @Builder.Default
    private Integer allocationVersion = 1;

    /**
     * Helper method to set bidirectional relationship with ETF
     * @param etf the ETF this allocation belongs to
     */
    public void setEtf(ETF etf) {
        this.etf = etf;
    }

    /**
     * Helper method to set bidirectional relationship with Stock
     * @param stock the stock this allocation references
     */
    public void setStock(Stock stock) {
        this.stock = stock;
    }
}
