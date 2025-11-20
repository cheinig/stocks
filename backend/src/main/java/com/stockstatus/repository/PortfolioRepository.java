package com.stockstatus.repository;

import com.stockstatus.domain.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Portfolio entity operations
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * Find all portfolios for a specific user
     * @param userId the user ID to find portfolios for
     * @return List of portfolios for that user
     */
    List<Portfolio> findByUserId(String userId);

    /**
     * Find all portfolios for a specific user with pagination
     * @param userId the user ID to find portfolios for
     * @param pageable pagination information
     * @return Page of portfolios for that user
     */
    Page<Portfolio> findByUserId(String userId, Pageable pageable);

    /**
     * Find a portfolio by user ID and name
     * @param userId the user ID
     * @param name the portfolio name
     * @return Optional containing the portfolio if found
     */
    Optional<Portfolio> findByUserIdAndName(String userId, String name);

    /**
     * Search portfolios by name (case-insensitive, partial match)
     * @param name the name to search for
     * @param pageable pagination information
     * @return Page of matching portfolios
     */
    @Query("SELECT p FROM Portfolio p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Portfolio> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Find all portfolios with their positions eagerly loaded
     * @return List of portfolios with positions
     */
    @Query("SELECT DISTINCT p FROM Portfolio p LEFT JOIN FETCH p.positions")
    List<Portfolio> findAllWithPositions();

    /**
     * Find a portfolio by ID with positions eagerly loaded
     * @param id the portfolio ID
     * @return Optional containing the portfolio with positions if found
     */
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.positions WHERE p.id = :id")
    Optional<Portfolio> findByIdWithPositions(@Param("id") Long id);

    /**
     * Count portfolios for a specific user
     * @param userId the user ID
     * @return number of portfolios for that user
     */
    long countByUserId(String userId);

    /**
     * Check if a portfolio with the given user ID and name exists
     * @param userId the user ID
     * @param name the portfolio name
     * @return true if a portfolio exists
     */
    boolean existsByUserIdAndName(String userId, String name);
}
