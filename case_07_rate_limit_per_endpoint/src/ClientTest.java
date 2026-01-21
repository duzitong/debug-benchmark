/**
 * Test program for API Client.
 * 
 * This test verifies that the client correctly handles rate limiting.
 * According to the API docs, we have 100 requests per minute available,
 * so making 5 requests to a single endpoint should work without issues.
 */
public class ClientTest {
    
    public static void main(String[] args) {
        System.out.println("=== API Client Rate Limiting Test ===");
        System.out.println();
        
        MockServer server = new MockServer();
        ApiClient client = new ApiClient(server);
        
        int successCount = 0;
        int failureCount = 0;
        int totalRequests = 5;
        
        System.out.println("Making " + totalRequests + " requests to /users endpoint");
        System.out.println("Expected: All requests succeed (we have 100 requests/min available)");
        System.out.println();
        
        for (int i = 1; i <= totalRequests; i++) {
            System.out.print("Request " + i + ": ");
            
            ApiResponse response = client.getUsers();
            
            if (response.isSuccess()) {
                System.out.println("SUCCESS (HTTP " + response.getStatusCode() + ")");
                successCount++;
            } else if (response.isRateLimited()) {
                System.out.println("RATE LIMITED (HTTP 429)");
                failureCount++;
            } else {
                System.out.println("ERROR (HTTP " + response.getStatusCode() + ")");
                failureCount++;
            }
        }
        
        System.out.println();
        System.out.println("=== Results ===");
        System.out.println("Successful requests: " + successCount);
        System.out.println("Failed requests: " + failureCount);
        System.out.println("Remaining client quota: " + client.getRemainingRequests());
        System.out.println();
        
        if (failureCount > 0) {
            System.out.println("ERROR: Received unexpected 429 responses!");
            System.out.println("The client rate limiter shows we should have quota remaining.");
            System.out.println("This indicates a mismatch between client expectations and server behavior.");
            System.exit(1);
        }
        
        System.out.println("All requests completed successfully.");
        System.exit(0);
    }
}
