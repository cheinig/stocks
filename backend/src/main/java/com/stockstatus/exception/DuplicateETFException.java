package com.stockstatus.exception;

/**
 * Exception thrown when trying to create an ETF with a duplicate ISIN
 */
public class DuplicateETFException extends DuplicateResourceException {

    public DuplicateETFException(String isin) {
        super("ETF", "isin", isin);
    }
}
