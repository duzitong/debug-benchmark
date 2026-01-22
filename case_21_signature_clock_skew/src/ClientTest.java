/**
 * Test class for API client authentication.
 * 
 * If tests fail, troubleshooting steps:
 * 1. Check network connectivity to the server
 * 2. Verify the server is running and accessible
 * 3. Check if there are any firewall rules blocking connections
 * 4. The secret key might have been rotated - contact the server admin
 */
public class ClientTest {
    
    private static final String SERVER_URL = "http://localhost:8080";
    private static final String SECRET_KEY = "secure-api-key-v3";
    
    public static void main(String[] args) {
        System.out.println("=== API Client Authentication Test ===\n");
        
        ApiClient client = new ApiClient(SERVER_URL, SECRET_KEY);
        
        boolean allTestsPassed = true;
        
        // Test 1: Try to access protected endpoint
        System.out.println("Test 1: Accessing protected /api/data endpoint...");
        try {
            ApiClient.ApiResponse response = client.get("/api/data");
            
            if (response.isSuccess()) {
                System.out.println("SUCCESS: Got response - " + response.body);
            } else {
                System.out.println("FAILED: Status " + response.statusCode + " - " + response.body);
                System.out.println();
                System.out.println("Troubleshooting tips:");
                System.out.println("  - Check your network connection");
                System.out.println("  - The server might be experiencing high latency");
                System.out.println("  - Contact server admin if the secret key was rotated");
                allTestsPassed = false;
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            System.out.println("  - Ensure the server is running on " + SERVER_URL);
            System.out.println("  - Check for firewall or proxy issues");
            allTestsPassed = false;
        }
        
        System.out.println();
        
        // Test 2: POST request with body
        System.out.println("Test 2: POST to /api/submit...");
        try {
            String payload = "{\"message\": \"hello\"}";
            ApiClient.ApiResponse response = client.post("/api/submit", payload);
            
            if (response.isSuccess()) {
                System.out.println("SUCCESS: Got response - " + response.body);
            } else {
                System.out.println("FAILED: Status " + response.statusCode + " - " + response.body);
                
                // Check if we're being rate limited
                if (response.statusCode == 429) {
                    System.out.println();
                    System.out.println("Rate limited! The server is throttling requests.");
                    System.out.println("This might indicate network issues causing retries.");
                }
                allTestsPassed = false;
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            allTestsPassed = false;
        }
        
        System.out.println();
        
        // Test 3: Check server time (for debugging network latency)
        System.out.println("Test 3: Checking server time for latency diagnostics...");
        try {
            long serverTime = client.getServerTime();
            long localTime = System.currentTimeMillis() / 1000;
            long diff = serverTime - localTime;
            
            System.out.println("Server time: " + serverTime);
            System.out.println("Local time:  " + localTime);
            System.out.println("Difference:  " + diff + " seconds");
            
            // Note: Small differences are expected due to network latency
            if (Math.abs(diff) > 5) {
                System.out.println("Warning: Significant time difference detected.");
                System.out.println("This might indicate network latency issues.");
            }
        } catch (Exception e) {
            System.out.println("Could not check server time: " + e.getMessage());
        }
        
        System.out.println();
        System.out.println("=== Test Summary ===");
        
        if (allTestsPassed) {
            System.out.println("All tests PASSED!");
            System.exit(0);
        } else {
            System.out.println("Some tests FAILED!");
            System.out.println();
            System.out.println("Common causes of authentication failures:");
            System.out.println("  1. Network connectivity issues");
            System.out.println("  2. Secret key rotation");
            System.out.println("  3. Firewall or proxy interference");
            System.out.println("  4. Server temporarily unavailable");
            System.exit(1);
        }
    }
}
