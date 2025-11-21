package com.stockstatus.exception;

/**
 * Exception thrown when trying to create a Stock with a duplicate ISIN
 */
public class DuplicateStockException extends DuplicateResourceException {

    public DuplicateStockException(String isin) {
        super("Stock", "isin", isin);
    }
}
