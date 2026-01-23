import java.util.Map;

public class ClientTest {
    
    private static final String SERVER_URL = "http://localhost:8080";
    private static final int NUM_REQUESTS = 5;
    
    public static void main(String[] args) {
        System.out.println("Starting session affinity test...");
        
        SessionClient client = new SessionClient(SERVER_URL);
        
        try {
            String userId = "user-" + System.currentTimeMillis();
            System.out.println("Creating session for user: " + userId);
            
            String sessionId = client.createSession(userId);
            if (sessionId == null || sessionId.isEmpty()) {
                System.err.println("ERROR: Failed to create session - no session ID returned");
                System.exit(1);
            }
            System.out.println("Session created: " + sessionId);
            
            int successCount = 0;
            int failureCount = 0;
            
            for (int i = 0; i < NUM_REQUESTS; i++) {
                try {
                    System.out.println("\nRequest " + (i + 1) + "/" + NUM_REQUESTS);
                    
                    String updateData = "data-" + i + "-" + System.currentTimeMillis();
                    boolean updated = client.updateSession(sessionId, updateData);
                    if (updated) {
                        System.out.println("  Update successful");
                    }
                    
                    Map<String, Object> sessionData = client.getSessionData(sessionId);
                    System.out.println("  Session data retrieved: " + sessionData.get("data"));
                    
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    System.err.println("  Request failed: " + e.getMessage());
                }
            }
            
            System.out.println("\n========================================");
            System.out.println("Test Results:");
            System.out.println("  Successful requests: " + successCount + "/" + NUM_REQUESTS);
            System.out.println("  Failed requests: " + failureCount + "/" + NUM_REQUESTS);
            System.out.println("========================================");
            
            if (failureCount > 0) {
                System.err.println("TEST FAILED: Some requests failed due to session routing issues");
                System.exit(1);
            }
            
            System.out.println("TEST PASSED: All requests completed successfully");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
