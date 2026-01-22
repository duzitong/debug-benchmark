import server.WebhookServer;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WebhookClient {
    private final WebhookServer server;
    private final List<WebhookServer.RetryEvent> retryEvents = new ArrayList<>();
    
    public WebhookClient(WebhookServer server) {
        this.server = server;
        this.server.setRetryListener(event -> retryEvents.add(event));
    }
    
    public void registerWebhook(String id, WebhookConfig config) {
        Map<String, Object> configMap = config.toMap();
        server.registerWebhook(id, configMap);
    }
    
    public void configureFailures(String id, int failCount) {
        server.configureFailures(id, failCount);
    }
    
    public void triggerWebhook(String id, String payload) {
        server.triggerWebhook(id, payload);
    }
    
    public List<WebhookServer.RetryEvent> getRetryEvents() {
        return new ArrayList<>(retryEvents);
    }
    
    public void clearRetryEvents() {
        retryEvents.clear();
    }
    
    public WebhookConfig createExponentialBackoffConfig(String url, String eventType, 
            int maxRetries, long initialDelayMs, double multiplier) {
        return WebhookConfig.builder()
                .url(url)
                .eventType(eventType)
                .maxRetries(maxRetries)
                .initialDelayMs(initialDelayMs)
                .retryFactor(multiplier)
                .backoffMode("EXPONENTIAL")
                .enableJitter(false)
                .build();
    }
}
