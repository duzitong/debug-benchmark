public class ClientTest {
    
    private static final String SERVER_URL = "http://localhost:8080";
    private static final int TEST_DURATION_SECONDS = 60;
    private static final double MINIMUM_SUCCESS_RATE = 0.92;
    
    public static void main(String[] args) {
        System.out.println("Starting API client performance test...");
        System.out.println("Test duration: " + TEST_DURATION_SECONDS + " seconds");
        System.out.println("Target success rate: " + (MINIMUM_SUCCESS_RATE * 100) + "%");
        System.out.println();
        
        // Start server
        Thread serverThread = new Thread(() -> {
            try {
                RateLimitedServer.main(new String[]{"8080"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        
        try {
            Thread.sleep(500); // Wait for server to start
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        ApiClient client = new ApiClient(SERVER_URL);
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (TEST_DURATION_SECONDS * 1000L);
        
        int totalRequests = 0;
        int successfulRequests = 0;
        int rateLimitedRequests = 0;
        
        try {
            while (System.currentTimeMillis() < endTime) {
                ApiClient.ApiResponse response = client.fetchResource();
                totalRequests++;
                
                if (response.isSuccess()) {
                    successfulRequests++;
                } else if (response.isRateLimited()) {
                    rateLimitedRequests++;
                }
                
                if (totalRequests % 10 == 0) {
                    double currentRate = (double) successfulRequests / totalRequests;
                    System.out.printf("Progress: %d requests, success rate: %.1f%%\n", 
                        totalRequests, currentRate * 100);
                }
            }
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        long actualDuration = System.currentTimeMillis() - startTime;
        double successRate = totalRequests > 0 ? (double) successfulRequests / totalRequests : 0;
        double throughput = (double) successfulRequests / (actualDuration / 1000.0);
        
        System.out.println();
        System.out.println("=== Test Results ===");
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Successful: " + successfulRequests);
        System.out.println("Rate limited: " + rateLimitedRequests);
        System.out.printf("Success rate: %.2f%%\n", successRate * 100);
        System.out.printf("Throughput: %.2f requests/second\n", throughput);
        
        if (successRate < MINIMUM_SUCCESS_RATE) {
            System.out.println();
            System.out.println("FAILED: Performance below expected threshold");
            System.out.println("The client rate limiting strategy is not aligned with the server.");
            System.exit(1);
        } else {
            System.out.println();
            System.out.println("PASSED: Performance meets expectations");
            System.exit(0);
        }
    }
}
