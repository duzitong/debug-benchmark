/**
 * Test for TransactionClient
 * 
 * This test verifies that transactional queries return consistent data.
 * When querying count and sum within the same transaction, the values
 * should be from the same snapshot of data.
 * 
 * Expected behavior:
 * - 5 items with values: 50, 75, 100, 125, 150
 * - Total sum: 500
 * - Average: 100
 */
public class ClientTest {
    
    // Expected values based on known test data
    private static final int EXPECTED_COUNT = 5;
    private static final int EXPECTED_SUM = 500;
    private static final double EXPECTED_AVERAGE = 100.0;
    
    public static void main(String[] args) {
        System.out.println("=== Transaction Isolation Test ===");
        System.out.println();
        
        TransactionClient client = new TransactionClient();
        
        try {
            // Start a transaction - this should give us a consistent view of the data
            System.out.println("Starting transaction...");
            client.beginTransaction();
            
            // Query 1: Get the count of items
            System.out.println("Executing Query 1: Getting item count...");
            int count = client.getItemCount();
            System.out.println("  Count: " + count);
            
            // In a real scenario, some time passes here
            // Another transaction might commit new data
            // But our transaction should still see the original snapshot
            System.out.println("  (Time passes... other transactions may commit)");
            simulateTimePassing();
            
            // Query 2: Get the sum of item values
            System.out.println("Executing Query 2: Getting item sum...");
            int sum = client.getItemSum();
            System.out.println("  Sum: " + sum);
            
            // Calculate average from the transaction's consistent view
            System.out.println();
            System.out.println("Calculating average (sum / count)...");
            double average = client.calculateAverage(count, sum);
            System.out.println("  Calculated average: " + average);
            System.out.println("  Expected average: " + EXPECTED_AVERAGE);
            
            // Commit the transaction
            client.commit();
            System.out.println();
            System.out.println("Transaction committed.");
            
            // Verify the result
            System.out.println();
            System.out.println("=== Verification ===");
            System.out.println("Count: " + count + " (expected: " + EXPECTED_COUNT + ")");
            System.out.println("Sum: " + sum + " (expected: " + EXPECTED_SUM + ")");
            System.out.println("Average: " + average + " (expected: " + EXPECTED_AVERAGE + ")");
            
            // Check if average matches expected value
            // Allow for small floating point differences
            if (Math.abs(average - EXPECTED_AVERAGE) > 0.001) {
                System.out.println();
                System.out.println("ERROR: Average calculation is incorrect!");
                System.out.println("This indicates inconsistent data was read within the transaction.");
                System.out.println("Possible cause: Phantom read - another transaction's data was visible");
                System.out.println();
                System.out.println("Details:");
                System.out.println("  If count=" + count + " and sum=" + sum + ", average should be " + average);
                System.out.println("  But we expected average=" + EXPECTED_AVERAGE + " (sum=" + EXPECTED_SUM + "/count=" + EXPECTED_COUNT + ")");
                System.exit(1);
            }
            
            System.out.println();
            System.out.println("SUCCESS: Transaction returned consistent data.");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
            if (client.isInTransaction()) {
                client.rollback();
            }
            System.exit(1);
        }
    }
    
    /**
     * Simulates time passing between queries.
     * In a real system, this represents the window where other transactions
     * might commit changes. Our transaction should be isolated from these changes.
     */
    private static void simulateTimePassing() {
        try {
            Thread.sleep(10); // Small delay to represent processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
