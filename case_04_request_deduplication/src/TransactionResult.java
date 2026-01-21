package com.payment.client;

/**
 * Represents the outcome of a payment transaction attempt.
 */
public class TransactionResult {
    
    private final boolean successful;
    private final String message;
    private final int retryCount;
    
    public TransactionResult(boolean successful, String message, int retryCount) {
        this.successful = successful;
        this.message = message;
        this.retryCount = retryCount;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public String getMessage() {
        return message;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
}
