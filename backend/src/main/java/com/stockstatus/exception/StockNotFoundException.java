package com.stockstatus.exception;

/**
 * Exception thrown when a Stock is not found
 */
public class StockNotFoundException extends ResourceNotFoundException {

    public StockNotFoundException(Long id) {
        super("Stock", "id", id);
    }

    public StockNotFoundException(String fieldName, Object fieldValue) {
        super("Stock", fieldName, fieldValue);
    }
}
