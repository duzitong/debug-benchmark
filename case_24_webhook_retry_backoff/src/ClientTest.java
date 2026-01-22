import server.WebhookServer;
import java.util.List;

public class ClientTest {
    
    public static void main(String[] args) {
        System.out.println("Testing Webhook Exponential Backoff...");
        System.out.println("========================================");
        
        WebhookServer server = new WebhookServer();
        WebhookClient client = new WebhookClient(server);
        
        WebhookConfig config = WebhookConfig.builder()
                .url("https://api.example.com/webhook")
                .eventType("order.created")
                .maxRetries(4)
                .initialDelayMs(100)
                .retryFactor(2.0)
                .backoffMode("EXPONENTIAL")
                .enableJitter(false)
                .build();
        
        System.out.println("Configuration:");
        System.out.println("  URL: " + config.getUrl());
        System.out.println("  Event Type: " + config.getEventType());
        System.out.println("  Max Retries: " + config.getMaxRetries());
        System.out.println("  Initial Delay: " + config.getInitialDelayMs() + "ms");
        System.out.println("  Retry Factor: " + config.getRetryFactor());
        System.out.println("  Backoff Mode: " + config.getBackoffMode());
        System.out.println("  Enable Jitter: " + config.isEnableJitter());
        System.out.println();
        
        client.registerWebhook("test-webhook", config);
        client.configureFailures("test-webhook", 3);
        
        System.out.println("Triggering webhook with 3 configured failures...");
        System.out.println("Expected delays with exponential backoff (factor=2.0):");
        System.out.println("  Retry 1: 100ms");
        System.out.println("  Retry 2: 200ms (100 * 2^1)");
        System.out.println("  Retry 3: 400ms (100 * 2^2)");
        System.out.println();
        
        client.triggerWebhook("test-webhook", "{\"orderId\": 12345}");
        
        List<WebhookServer.RetryEvent> events = client.getRetryEvents();
        
        System.out.println("Actual retry events:");
        for (WebhookServer.RetryEvent event : events) {
            System.out.println("  Retry " + event.attemptNumber + ": " + event.delayMs + "ms");
        }
        System.out.println();
        
        boolean testPassed = verifyExponentialBackoff(events, 100, 2.0);
        
        if (testPassed) {
            System.out.println("SUCCESS: Exponential backoff is working correctly!");
            System.exit(0);
        } else {
            System.out.println("FAILURE: Delays are NOT following exponential backoff pattern!");
            System.out.println("The retryFactor and backoffMode settings don't seem to be working.");
            System.exit(1);
        }
    }
    
    private static boolean verifyExponentialBackoff(List<WebhookServer.RetryEvent> events, 
            long initialDelay, double expectedFactor) {
        if (events.size() < 2) {
            System.out.println("Not enough retry events to verify exponential pattern");
            return false;
        }
        
        for (int i = 1; i < events.size(); i++) {
            long prevDelay = events.get(i - 1).delayMs;
            long currDelay = events.get(i).delayMs;
            
            double actualRatio = (double) currDelay / prevDelay;
            
            System.out.println("Checking ratio between retry " + i + " and " + (i + 1) + 
                    ": " + currDelay + "/" + prevDelay + " = " + actualRatio);
            
            if (Math.abs(actualRatio - expectedFactor) > 0.1) {
                System.out.println("Expected ratio: " + expectedFactor + ", got: " + actualRatio);
                return false;
            }
        }
        
        return true;
    }
}
