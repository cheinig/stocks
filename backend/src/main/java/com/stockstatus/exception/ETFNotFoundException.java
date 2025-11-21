package com.stockstatus.exception;

/**
 * Exception thrown when an ETF is not found
 */
public class ETFNotFoundException extends ResourceNotFoundException {

    public ETFNotFoundException(Long id) {
        super("ETF", "id", id);
    }

    public ETFNotFoundException(String fieldName, Object fieldValue) {
        super("ETF", fieldName, fieldValue);
    }
}
