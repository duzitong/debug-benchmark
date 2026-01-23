import java.io.File;
import java.io.IOException;

/**
 * ClientTest - Demonstrates the deadline propagation bug.
 * 
 * This test shows that when the client sets a 5-second deadline:
 * - ServiceA takes 1 second
 * - ServiceB takes 4 seconds (no deadline propagation)
 * - Total: ~5 seconds, which causes deadline to be exceeded
 * 
 * The bug is that ServiceA does NOT propagate the remaining deadline
 * (4 seconds after ServiceA completes) to ServiceB. Instead, ServiceB
 * uses its own default 30-second timeout.
 * 
 * Exit codes:
 * - 0: Test passed (bug is fixed - request completed within deadline)
 * - 1: Bug detected (deadline exceeded or timeout)
 */
public class ClientTest {
    
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "localhost";
    
    private static Process serverProcess;
    
    public static void main(String[] args) {
        System.out.println("=== gRPC Deadline Propagation Bug Test ===\n");
        
        try {
            // Start the server
            if (!startServer()) {
                System.err.println("Failed to start server");
                System.exit(1);
            }
            
            // Wait for server to be ready
            Thread.sleep(2000);
            
            // Run the test
            boolean bugDetected = runDeadlinePropagationTest();
            
            if (bugDetected) {
                System.out.println("\n=== BUG DETECTED ===");
                System.out.println("The deadline was exceeded because ServiceA does not");
                System.out.println("propagate the remaining deadline to ServiceB.");
                System.out.println("ServiceB uses its own 30-second default timeout,");
                System.out.println("ignoring the client's 5-second deadline.");
                System.exit(1);
            } else {
                System.out.println("\n=== TEST PASSED ===");
                System.out.println("Request completed within deadline.");
                System.exit(0);
            }
            
        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            stopServer();
        }
    }
    
    /**
     * Run the deadline propagation test.
     * 
     * Expected behavior (buggy):
     * - Client sets 5-second deadline
     * - ServiceA takes 1s, should leave 4s for ServiceB
     * - But ServiceA doesn't propagate deadline
     * - ServiceB takes 4s with its own timeout
     * - Total ~5s + overhead = deadline exceeded
     * 
     * @return true if bug is detected (deadline exceeded), false if working correctly
     */
    private static boolean runDeadlinePropagationTest() {
        GrpcClient client = new GrpcClient(SERVER_HOST, SERVER_PORT);
        
        // Wait for server health check
        System.out.println("Checking server health...");
        int retries = 10;
        while (retries > 0 && !client.isHealthy()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            retries--;
        }
        
        if (!client.isHealthy()) {
            System.err.println("Server is not healthy after waiting");
            return true; // Treat as bug for exit code 1
        }
        
        System.out.println("Server is healthy\n");
        
        // Test 1: Make a request with 5-second deadline
        System.out.println("Test: Making request with 5-second deadline");
        System.out.println("Expected: ServiceA (1s) + ServiceB (4s) = 5s total");
        System.out.println("Reality: No deadline propagation, timing causes failure\n");
        
        long testStartTime = System.currentTimeMillis();
        
        try {
            // This should fail because:
            // - Client read timeout is 5 seconds
            // - ServiceA takes 1s
            // - ServiceB takes 4s
            // - Total is right at the edge, causing deadline to be exceeded
            GrpcClient.GrpcResponse response = client.processWithDefaultDeadline("test-request");
            
            long elapsed = System.currentTimeMillis() - testStartTime;
            
            System.out.println("Response received:");
            System.out.println("  Status code: " + response.statusCode);
            System.out.println("  Elapsed: " + elapsed + "ms");
            System.out.println("  Body: " + response.body);
            
            // Check if deadline was exceeded (server returns 504)
            if (response.isDeadlineExceeded()) {
                System.out.println("\nDeadline exceeded - server reported timeout");
                return true; // Bug detected
            }
            
            // Even if response came back, check if it took longer than expected
            // The bug manifests as response taking >= 5 seconds due to no propagation
            if (elapsed >= 5000) {
                System.out.println("\nResponse took " + elapsed + "ms (>= 5000ms deadline)");
                System.out.println("This indicates the deadline was not respected.");
                return true; // Bug detected
            }
            
            // If we got a success response in time, bug might be fixed
            if (response.isSuccess() && elapsed < 5000) {
                System.out.println("\nRequest completed in " + elapsed + "ms (under deadline)");
                return false; // No bug - working correctly
            }
            
            // Server error other than timeout
            System.out.println("\nServer returned error: " + response.statusCode);
            return true;
            
        } catch (GrpcClient.GrpcException e) {
            long elapsed = System.currentTimeMillis() - testStartTime;
            
            System.out.println("Exception occurred:");
            System.out.println("  Status: " + e.status);
            System.out.println("  Message: " + e.getMessage());
            System.out.println("  Elapsed: " + elapsed + "ms");
            
            if (e.status.equals("DEADLINE_EXCEEDED")) {
                System.out.println("\nClient-side timeout - deadline not respected by server");
                return true; // Bug detected
            }
            
            // Other errors
            System.out.println("\nUnexpected error: " + e);
            return true;
        }
    }
    
    /**
     * Start the server process.
     */
    private static boolean startServer() {
        try {
            // Get the path to server classes
            String classPath = System.getProperty("java.class.path");
            
            // Start server in background
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", classPath, "GrpcServer", String.valueOf(SERVER_PORT)
            );
            
            pb.directory(new File("."));
            pb.inheritIO();
            
            serverProcess = pb.start();
            System.out.println("Server starting on port " + SERVER_PORT + "...");
            
            return true;
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Stop the server process.
     */
    private static void stopServer() {
        if (serverProcess != null) {
            serverProcess.destroyForcibly();
            System.out.println("\nServer stopped");
        }
    }
}
