package com.stockstatus.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ETF Entity - represents an Exchange-Traded Fund
 * Maps to the 'etfs' table in the database
 */
@Entity
@Table(name = "etfs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "allocations")
@EqualsAndHashCode(of = "id")
public class ETF {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "ETF name is required")
    @Size(max = 255, message = "ETF name must not exceed 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "ISIN is required")
    @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{9}[0-9]$", message = "ISIN must be a valid format (e.g., IE00B4L5Y983)")
    @Column(name = "isin", nullable = false, unique = true, length = 12)
    private String isin;

    @NotNull(message = "Importer type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "importer_type", nullable = false, length = 50)
    private ImporterType importerType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "etf", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ETFAllocation> allocations = new ArrayList<>();

    /**
     * Helper method to add an allocation to this ETF
     * @param allocation the allocation to add
     */
    public void addAllocation(ETFAllocation allocation) {
        allocations.add(allocation);
        allocation.setEtf(this);
    }

    /**
     * Helper method to remove an allocation from this ETF
     * @param allocation the allocation to remove
     */
    public void removeAllocation(ETFAllocation allocation) {
        allocations.remove(allocation);
        allocation.setEtf(null);
    }
}
