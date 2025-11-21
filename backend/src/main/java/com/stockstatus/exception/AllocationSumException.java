package com.stockstatus.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when the sum of allocation percentages is invalid
 */
public class AllocationSumException extends RuntimeException {

    public AllocationSumException(String message) {
        super(message);
    }

    public AllocationSumException(BigDecimal actualSum) {
        super(String.format("Invalid allocation sum: %.2f%%. Expected sum should be close to 100%%", actualSum));
    }

    public AllocationSumException(BigDecimal actualSum, BigDecimal expectedSum) {
        super(String.format("Invalid allocation sum: %.2f%%. Expected: %.2f%%", actualSum, expectedSum));
    }
}
