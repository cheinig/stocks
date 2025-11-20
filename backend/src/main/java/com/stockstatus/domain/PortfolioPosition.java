package com.stockstatus.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PortfolioPosition Entity - represents a single position within a portfolio
 * Can reference either a Stock or an ETF
 * Maps to the 'portfolio_positions' table in the database
 */
@Entity
@Table(name = "portfolio_positions",
    indexes = {
        @Index(name = "idx_portfolio_positions_portfolio", columnList = "portfolio_id"),
        @Index(name = "idx_portfolio_positions_asset", columnList = "asset_type, asset_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_portfolio_position", columnNames = {"portfolio_id", "asset_type", "asset_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "portfolio")
@EqualsAndHashCode(of = "id")
public class PortfolioPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Portfolio reference is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_position_portfolio"))
    private Portfolio portfolio;

    @NotNull(message = "Asset type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @NotNull(message = "Asset ID is required")
    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.000001", message = "Quantity must be greater than 0")
    @Column(name = "quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Helper method to set bidirectional relationship with Portfolio
     * @param portfolio the portfolio this position belongs to
     */
    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    /**
     * Check if this position references a Stock
     * @return true if this is a stock position
     */
    public boolean isStockPosition() {
        return AssetType.STOCK.equals(assetType);
    }

    /**
     * Check if this position references an ETF
     * @return true if this is an ETF position
     */
    public boolean isEtfPosition() {
        return AssetType.ETF.equals(assetType);
    }
}
