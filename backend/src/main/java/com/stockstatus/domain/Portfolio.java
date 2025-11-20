package com.stockstatus.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Portfolio Entity - represents a user's investment portfolio
 * Contains multiple positions of stocks and ETFs
 * Maps to the 'portfolios' table in the database
 */
@Entity
@Table(name = "portfolios", indexes = {
    @Index(name = "idx_portfolios_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "positions")
@EqualsAndHashCode(of = "id")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 100, message = "User ID must not exceed 100 characters")
    @Column(name = "user_id", length = 100)
    private String userId;

    @NotBlank(message = "Portfolio name is required")
    @Size(max = 255, message = "Portfolio name must not exceed 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PortfolioPosition> positions = new ArrayList<>();

    /**
     * Helper method to add a position to this portfolio
     * @param position the position to add
     */
    public void addPosition(PortfolioPosition position) {
        positions.add(position);
        position.setPortfolio(this);
    }

    /**
     * Helper method to remove a position from this portfolio
     * @param position the position to remove
     */
    public void removePosition(PortfolioPosition position) {
        positions.remove(position);
        position.setPortfolio(null);
    }

    /**
     * Helper method to clear all positions
     */
    public void clearPositions() {
        positions.forEach(position -> position.setPortfolio(null));
        positions.clear();
    }
}
