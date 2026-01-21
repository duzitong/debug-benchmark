package com.payment.client;

import com.payment.server.PaymentException;
import com.payment.server.PaymentResult;
import com.payment.server.PaymentServer;

/**
 * Client for initiating payment transactions.
 * Handles communication with the payment server and implements retry logic.
 */
public class PaymentClient {
    
    private final PaymentServer server;
    private final String sourceAccount;
    private final String destinationAccount;
    private final int maxRetries;
    
    // Tracks retry attempts for error reporting
    private int lastRetryCount;
    
    /**
     * Creates a new payment client.
     * @param server the payment server instance
     * @param sourceAccount the account to debit
     * @param destinationAccount the account to credit
     * @param maxRetries maximum retry attempts for transient failures
     */
    public PaymentClient(PaymentServer server, String sourceAccount, 
                         String destinationAccount, int maxRetries) {
        this.server = server;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.maxRetries = maxRetries;
    }
    
    /**
     * Initiates a payment transaction with automatic retry on transient failures.
     * @param paymentRef unique payment reference for idempotency
     * @param amount the payment amount
     * @return the result of the transaction
     */
    public TransactionResult initiatePayment(String paymentRef, double amount) {
        lastRetryCount = 0;
        PaymentResult result = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                result = server.processPayment(paymentRef, sourceAccount, 
                                               destinationAccount, amount);
                
                if (result.isSuccess()) {
                    return new TransactionResult(true, "Payment completed", lastRetryCount);
                } else {
                    // Payment was not successful - report failure
                    return new TransactionResult(false, result.getMessage(), lastRetryCount);
                }
                
            } catch (PaymentException e) {
                lastRetryCount = attempt + 1;
                
                if (!e.isRetryable() || attempt >= maxRetries) {
                    // Non-retryable error or max retries exceeded
                    return new TransactionResult(false, 
                        "Payment failed: " + e.getMessage(), lastRetryCount);
                }
                
                // Retry the payment with the same reference
            }
        }
        
        return new TransactionResult(false, "Payment failed after retries", lastRetryCount);
    }
}
