/**
 * Test program for CORS API Client functionality.
 * 
 * This test validates the client behavior when server policy changes dynamically.
 * The server uses a request-count based policy that the client cannot predict.
 * 
 * Test Scenario:
 * 1. First POST request: Preflight + Actual = 2 server requests (POST allowed)
 * 2. Second POST request: Uses cached preflight + Actual = 1 server request (POST allowed)
 * 3. Third POST request: Uses cached preflight + Actual = 1 server request
 *    - At this point, total server requests = 5 (threshold reached)
 *    - POST is removed from allowed methods
 *    - Actual request fails with CORS error
 *    - Client's cached preflight is stale
 * 
 * The test verifies proper cache invalidation and retry behavior.
 */
public class ClientTest {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("CORS API Client Integration Test Suite");
        System.out.println("Request-Count Based Policy Change Test");
        System.out.println("=".repeat(70));
        System.out.println();
        printConfiguration();
        System.out.println("-".repeat(70));
        
        CorsServer server = new CorsServer();
        CorsApiClient client = new CorsApiClient(server, "https://webapp.example.com");
        
        // Configure client with reasonable settings
        client.setConnectionTimeout(30000);
        client.setConnectionPoolEnabled(true);
        client.setMaxPoolSize(10);
        client.setMaxRequestsPerMinute(100);
        
        String apiEndpoint = "https://api.server.com/data";
        boolean criticalFailure = false;
        
        try {
            // Request 1: First POST (preflight + actual = 2 server requests)
            runTest1(client, apiEndpoint);
            
            // Request 2: Second POST (cached preflight + actual = 1 server request)
            runTest2(client, apiEndpoint);
            
            // Request 3: Third POST (cached preflight + actual)
            // This triggers the bug - server policy has changed
            criticalFailure = runTest3(client, apiEndpoint);
            
        } catch (Exception e) {
            System.out.println("[FATAL] Unhandled exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
        
        // Print summary
        printTestSummary();
        
        if (criticalFailure) {
            System.out.println("Critical failure detected - client encountered unexpected CORS error.");
            System.out.println();
            System.out.println("Analysis:");
            System.out.println("  - The client cached the preflight response from the first request");
            System.out.println("  - Server policy changed dynamically (POST no longer allowed)");
            System.out.println("  - Client's retry mechanism invalidated cache but lookups still found data");
            System.out.println("  - Each retry attempt used the same outdated cached preflight");
            System.out.println();
            printConnectionStats(client);
            System.out.println();
            System.out.println("Exiting with error code 1");
            System.out.println("=".repeat(70));
            System.exit(1);
        }
        
        System.out.println("All tests completed successfully.");
        System.out.println("=".repeat(70));
        System.exit(0);
    }
    
    private static void printConfiguration() {
        System.out.println("Test Configuration:");
        System.out.println("  - Server policy change threshold: After 5 total requests");
        System.out.println("  - Cache hint duration: 3600 seconds");
        System.out.println("  - Client retry attempts: 3");
        System.out.println("  - Connection pool enabled: true");
        System.out.println();
        System.out.println("Expected Request Flow:");
        System.out.println("  Request 1: OPTIONS (1) + POST (2) = server sees 2 requests");
        System.out.println("  Request 2: [cached] + POST (3) = server sees 3 requests");  
        System.out.println("  Request 3: [cached] + POST (4,5,6) = threshold exceeded at request 5");
        System.out.println();
    }
    
    /**
     * Test 1: First POST request
     * Triggers preflight and actual request. Total: 2 server requests.
     * Server has POST allowed (count < 5).
     */
    private static void runTest1(CorsApiClient client, String apiEndpoint) {
        System.out.println("\n[TEST 1] First POST Request");
        System.out.println("Expected: Fresh preflight + POST should succeed");
        System.out.println("Server request count after: 2 (preflight + actual)\n");
        
        long startTime = System.currentTimeMillis();
        
        try {
            ApiResponse response = client.sendRequest("POST", apiEndpoint, "{\"action\":\"create\"}");
            
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("[TIMING] Request completed in " + elapsed + "ms");
            System.out.println("[RESULT] Status: " + response.getStatusCode() + 
                    ", Body: " + response.getBody());
            
            if (response.getStatusCode() == 200) {
                System.out.println("[TEST 1] PASSED");
                testsPassed++;
            } else {
                System.out.println("[TEST 1] FAILED - Unexpected status code");
                testsFailed++;
            }
        } catch (CorsException e) {
            System.out.println("[TEST 1] FAILED - CORS Exception: " + e.getMessage());
            System.out.println("  Error type: " + e.getCorsErrorType());
            testsFailed++;
        }
        
        System.out.println("-".repeat(70));
    }
    
    /**
     * Test 2: Second POST request  
     * Uses cached preflight, sends actual request. Total: 3 server requests.
     * Server still has POST allowed (count < 5).
     */
    private static void runTest2(CorsApiClient client, String apiEndpoint) {
        System.out.println("\n[TEST 2] Second POST Request (Uses Cached Preflight)");
        System.out.println("Expected: Uses cached preflight, POST should succeed");
        System.out.println("Server request count after: 3 (previous 2 + actual)\n");
        
        long startTime = System.currentTimeMillis();
        
        try {
            ApiResponse response = client.sendRequest("POST", apiEndpoint, "{\"action\":\"update\"}");
            
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("[TIMING] Request completed in " + elapsed + "ms");
            System.out.println("[RESULT] Status: " + response.getStatusCode() + 
                    ", Body: " + response.getBody());
            
            if (response.getStatusCode() == 200) {
                System.out.println("[TEST 2] PASSED - Successfully used cached preflight");
                testsPassed++;
            } else {
                System.out.println("[TEST 2] FAILED - Unexpected status code");
                testsFailed++;
            }
        } catch (CorsException e) {
            System.out.println("[TEST 2] FAILED - CORS Exception: " + e.getMessage());
            System.out.println("  Error type: " + e.getCorsErrorType());
            testsFailed++;
        }
        
        System.out.println("-".repeat(70));
    }
    
    /**
     * Test 3: Third POST request
     * Uses cached preflight, but server policy changes after 5th request.
     * 
     * Flow:
     * - Client checks cache: valid (max-age not expired)
     * - Client sends POST to server (request #4)
     * - Still works, but server is at threshold
     * - With retries, request #5 will trigger policy change
     * - Server rejects POST on this or subsequent retry
     * - Client retries but cache isn't invalidated
     * 
     * Actually: Request 4 is processed, but the 5th request (during retry or 
     * the next call) triggers the policy change.
     */
    private static boolean runTest3(CorsApiClient client, String apiEndpoint) {
        System.out.println("\n[TEST 3] Third POST Request (Policy Change Expected)");
        System.out.println("Server reaches request threshold (5 requests).");
        System.out.println("Client's cache is still valid according to max-age.");
        System.out.println("Expected: Server will reject POST, client should handle gracefully\n");
        
        long startTime = System.currentTimeMillis();
        boolean failureDetected = false;
        
        try {
            ApiResponse response = client.sendRequest("POST", apiEndpoint, "{\"action\":\"delete\"}");
            
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("[TIMING] Request completed in " + elapsed + "ms");
            System.out.println("[RESULT] Status: " + response.getStatusCode() + 
                    ", Body: " + response.getBody());
            
            // If we get here, the request succeeded - check if this is expected
            System.out.println("[TEST 3] Request succeeded");
            
            // Try one more to ensure we hit the threshold
            System.out.println("\n[TEST 3 continued] Fourth POST Request...\n");
            response = client.sendRequest("POST", apiEndpoint, "{\"action\":\"verify\"}");
            
            elapsed = System.currentTimeMillis() - startTime;
            System.out.println("[TIMING] Request completed in " + elapsed + "ms");
            System.out.println("[RESULT] Status: " + response.getStatusCode() + 
                    ", Body: " + response.getBody());
            
            System.out.println("[TEST 3] PASSED - All requests succeeded");
            testsPassed++;
            
        } catch (CorsException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("[TIMING] Request failed after " + elapsed + "ms");
            System.out.println("[RESULT] CORS ERROR: " + e.getMessage());
            System.out.println("  Error type: " + e.getCorsErrorType());
            System.out.println();
            System.out.println("[TEST 3] FAILED - Request was blocked");
            System.out.println("  Root Cause Analysis:");
            System.out.println("  - Client's cached preflight indicated POST was allowed");
            System.out.println("  - Server policy changed after reaching request threshold");
            System.out.println("  - Client invalidated cache entry but retry still used stale data");
            System.out.println("  - Retry mechanism repeated the same failing request");
            System.out.println("  - A proper fix would invalidate the correct cache on CORS error");
            testsFailed++;
            failureDetected = true;
        }
        
        System.out.println("-".repeat(70));
        return failureDetected;
    }
    
    private static void printTestSummary() {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("Test Summary:");
        System.out.println("  Passed: " + testsPassed);
        System.out.println("  Failed: " + testsFailed);
        System.out.println("  Total:  " + (testsPassed + testsFailed));
        System.out.println();
    }
    
    private static void printConnectionStats(CorsApiClient client) {
        System.out.println("Connection Statistics:");
        var stats = client.getConnectionStats();
        for (var entry : stats.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("  cacheSize: " + client.getCacheSize());
    }
}
