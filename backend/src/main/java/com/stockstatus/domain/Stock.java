package com.stockstatus.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stock Entity - represents a single stock/asset
 * Maps to the 'stocks' table in the database
 */
@Entity
@Table(name = "stocks", indexes = {
    @Index(name = "idx_stocks_country", columnList = "country"),
    @Index(name = "idx_stocks_sector", columnList = "sector"),
    @Index(name = "idx_stocks_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Stock name is required")
    @Size(max = 255, message = "Stock name must not exceed 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "ISIN is required")
    @Pattern(regexp = "^[A-Z]{2}[A-Z0-9]{9}[0-9]$", message = "ISIN must be a valid format (e.g., US0378331005)")
    @Column(name = "isin", nullable = false, unique = true, length = 12)
    private String isin;

    @NotBlank(message = "Country is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a valid ISO 3166-1 alpha-2 code")
    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Size(max = 100, message = "Sector must not exceed 100 characters")
    @Column(name = "sector", length = 100)
    private String sector;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
