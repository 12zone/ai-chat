package org.example.qasystem.exception;

public class UpstreamServiceException extends RuntimeException {
    private final int statusCode;
    private final String safeMessage;

    public UpstreamServiceException(int statusCode, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.statusCode = statusCode;
        this.safeMessage = safeMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getSafeMessage() {
        return safeMessage;
    }
}
