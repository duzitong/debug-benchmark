package com.payment.client;

import com.payment.server.PaymentException;
import com.payment.server.PaymentResult;
import com.payment.server.PaymentServer;

/**
 * Test harness for payment client functionality.
 * Tests payment processing with simulated network failures.
 */
public class ClientTest {
    
    private static boolean testFailed = false;
    
    public static void main(String[] args) {
        System.out.println("Payment Client Test Suite");
        System.out.println("=========================\n");
        
        testSuccessfulPayment();
        testPaymentWithRetry();
        testDuplicatePaymentHandling();
        
        System.out.println("\n=========================");
        if (testFailed) {
            System.out.println("RESULT: TESTS FAILED");
            System.exit(1);
        } else {
            System.out.println("RESULT: ALL TESTS PASSED");
            System.exit(0);
        }
    }
    
    /**
     * Tests a basic successful payment without failures.
     */
    static void testSuccessfulPayment() {
        System.out.println("Test: Successful Payment");
        
        PaymentServer server = new PaymentServer();
        server.registerAccount("ACC001", 1000.0);
        server.registerAccount("ACC002", 0.0);
        
        PaymentClient client = new PaymentClient(server, "ACC001", "ACC002", 3);
        TransactionResult result = client.initiatePayment("PAY-001", 250.0);
        
        // Verify result and balance through direct server access
        double senderBalance = server.getBalance("ACC001");
        double receiverBalance = server.getBalance("ACC002");
        
        if (result.isSuccessful() && senderBalance == 750.0 && receiverBalance == 250.0) {
            System.out.println("  PASSED: Payment successful, balances correct");
        } else {
            System.out.println("  FAILED: Unexpected result or balances");
            testFailed = true;
        }
    }
    
    /**
     * Tests payment processing when transient failures occur.
     * The server fails on the first attempt but succeeds on retry.
     */
    static void testPaymentWithRetry() {
        System.out.println("Test: Payment With Retry After Failure");
        
        // Create server that fails on first attempt
        FailingPaymentServer server = new FailingPaymentServer(1);
        server.registerAccount("ACC001", 500.0);
        server.registerAccount("ACC002", 100.0);
        
        PaymentClient client = new PaymentClient(server, "ACC001", "ACC002", 3);
        TransactionResult result = client.initiatePayment("PAY-002", 200.0);
        
        // Verify through direct server access
        double senderBalance = server.getBalance("ACC001");
        double receiverBalance = server.getBalance("ACC002");
        
        if (result.isSuccessful() && senderBalance == 300.0 && receiverBalance == 300.0) {
            System.out.println("  PASSED: Retry succeeded, balances correct");
        } else {
            System.out.println("  FAILED: sender=" + senderBalance + 
                             ", receiver=" + receiverBalance);
            testFailed = true;
        }
    }
    
    /**
     * Tests duplicate payment detection after a failure followed by retry.
     * This simulates a scenario where a payment might be processed but the
     * response is lost, triggering a retry.
     */
    static void testDuplicatePaymentHandling() {
        System.out.println("Test: Duplicate Payment After Network Failure");
        
        // Server processes payment but throws exception on first call
        FailOnSuccessServer server = new FailOnSuccessServer();
        server.registerAccount("ACC001", 1000.0);
        server.registerAccount("ACC002", 0.0);
        
        PaymentClient client = new PaymentClient(server, "ACC001", "ACC002", 3);
        TransactionResult result = client.initiatePayment("PAY-003", 400.0);
        
        // The payment should have succeeded exactly once
        double senderBalance = server.getBalance("ACC001");
        double receiverBalance = server.getBalance("ACC002");
        
        System.out.println("  Result: " + result.getMessage());
        System.out.println("  Sender balance: " + senderBalance + " (expected: 600.0)");
        System.out.println("  Receiver balance: " + receiverBalance + " (expected: 400.0)");
        
        if (result.isSuccessful() && senderBalance == 600.0 && receiverBalance == 400.0) {
            System.out.println("  PASSED: Duplicate handled correctly");
        } else {
            System.out.println("  FAILED: Incorrect duplicate handling");
            testFailed = true;
        }
    }
    
    /**
     * A payment server that fails for a specified number of attempts.
     */
    static class FailingPaymentServer extends PaymentServer {
        private int failuresRemaining;
        
        FailingPaymentServer(int failCount) {
            this.failuresRemaining = failCount;
        }
        
        @Override
        public PaymentResult processPayment(String paymentRef, String fromAccount,
                                           String toAccount, double amount) throws PaymentException {
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw new PaymentException("Connection timeout", true);
            }
            return super.processPayment(paymentRef, fromAccount, toAccount, amount);
        }
    }
    
    /**
     * A payment server that processes the payment but throws an exception
     * after processing, simulating a response delivery failure.
     */
    static class FailOnSuccessServer extends PaymentServer {
        private boolean hasThrown = false;
        
        @Override
        public PaymentResult processPayment(String paymentRef, String fromAccount,
                                           String toAccount, double amount) throws PaymentException {
            if (!hasThrown) {
                // Process the payment first
                PaymentResult result = super.processPayment(paymentRef, fromAccount, toAccount, amount);
                
                if (result.isSuccess()) {
                    // Payment went through, but simulate network failure on response
                    hasThrown = true;
                    throw new PaymentException("Connection lost after processing", true);
                }
                return result;
            }
            // Subsequent calls proceed normally
            return super.processPayment(paymentRef, fromAccount, toAccount, amount);
        }
    }
}
