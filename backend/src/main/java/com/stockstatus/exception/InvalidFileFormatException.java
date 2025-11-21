package com.stockstatus.exception;

/**
 * Exception thrown when an uploaded file has an invalid format
 */
public class InvalidFileFormatException extends RuntimeException {

    public InvalidFileFormatException(String message) {
        super(message);
    }

    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFileFormatException(String fileType, String reason) {
        super(String.format("Invalid %s file format: %s", fileType, reason));
    }

    public InvalidFileFormatException(String fileType, String reason, Throwable cause) {
        super(String.format("Invalid %s file format: %s", fileType, reason), cause);
    }
}
