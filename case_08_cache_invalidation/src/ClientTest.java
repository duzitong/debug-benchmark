/**
 * Test program to verify profile update and retrieval.
 * Demonstrates a data consistency issue after updates.
 */
public class ClientTest {
    
    private static final String SERVER_URL = "http://localhost:8080";
    
    public static void main(String[] args) {
        System.out.println("=== Profile API Client Test ===\n");
        
        ApiClient client = new ApiClient(SERVER_URL);
        String testUserId = "user_" + System.currentTimeMillis();
        
        try {
            // Step 1: Create a profile
            System.out.println("Step 1: Creating profile...");
            ProfileData created = client.createProfile(testUserId, "John Doe", "john@example.com");
            System.out.println("Created: " + created);
            long createdLastModified = created.getLastModified();
            
            // Small delay to simulate real usage pattern
            Thread.sleep(100);
            
            // Step 2: Fetch to populate cache
            System.out.println("\nStep 2: Initial fetch (populates cache)...");
            ProfileData firstFetch = client.getProfile(testUserId);
            System.out.println("Fetched: " + firstFetch);
            
            // Step 3: Update the profile
            Thread.sleep(100);
            System.out.println("\nStep 3: Updating profile...");
            ProfileData updated = client.updateProfile(testUserId, "Jane Smith", "jane@example.com");
            System.out.println("Updated: " + updated);
            long expectedLastModified = updated.getLastModified();
            int expectedVersion = updated.getVersion();
            
            System.out.println("Expected lastModified after update: " + expectedLastModified);
            System.out.println("Expected version after update: " + expectedVersion);
            
            // Step 4: Fetch again to verify update
            Thread.sleep(50);
            System.out.println("\nStep 4: Verifying data...");
            ProfileData fetched = client.getProfile(testUserId);
            System.out.println("Fetched after update: " + fetched);
            
            long actualLastModified = fetched.getLastModified();
            int actualVersion = fetched.getVersion();
            String actualName = fetched.getName();
            
            System.out.println("\n--- Verification ---");
            System.out.println("Expected lastModified: " + expectedLastModified);
            System.out.println("Got lastModified: " + actualLastModified);
            System.out.println("Expected name: Jane Smith");
            System.out.println("Got name: " + actualName);
            System.out.println("Expected version: " + expectedVersion);
            System.out.println("Got version: " + actualVersion);
            
            // Check if we got stale data
            boolean isStale = actualLastModified != expectedLastModified;
            boolean hasWrongName = !"Jane Smith".equals(actualName);
            boolean hasWrongVersion = actualVersion != expectedVersion;
            
            if (isStale || hasWrongName || hasWrongVersion) {
                System.out.println("\n=== TEST FAILED ===");
                System.out.println("Received stale/cached data after update!");
                
                if (isStale) {
                    System.out.println("- lastModified mismatch: expected " + expectedLastModified + 
                                     ", got " + actualLastModified);
                }
                if (hasWrongName) {
                    System.out.println("- Name mismatch: expected 'Jane Smith', got '" + actualName + "'");
                }
                if (hasWrongVersion) {
                    System.out.println("- Version mismatch: expected " + expectedVersion + 
                                     ", got " + actualVersion);
                }
                
                System.exit(1);
            } else {
                System.out.println("\n=== TEST PASSED ===");
                System.out.println("Data is consistent after update.");
                System.exit(0);
            }
            
        } catch (Exception e) {
            System.out.println("\n=== TEST ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
