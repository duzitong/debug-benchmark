import java.util.HashMap;
import java.util.Map;

public class WebhookConfig {
    private final Map<String, Object> config;
    
    private WebhookConfig(Builder builder) {
        this.config = new HashMap<>();
        this.config.put("url", builder.url);
        this.config.put("eventType", builder.eventType);
        this.config.put("maxRetries", builder.maxRetries);
        this.config.put("initialDelayMs", builder.initialDelayMs);
        this.config.put("retryFactor", builder.retryFactor);
        this.config.put("backoffMode", builder.backoffMode);
        this.config.put("enableJitter", builder.enableJitter);
    }
    
    public Map<String, Object> toMap() {
        return new HashMap<>(config);
    }
    
    public String getUrl() {
        return (String) config.get("url");
    }
    
    public String getEventType() {
        return (String) config.get("eventType");
    }
    
    public int getMaxRetries() {
        return (int) config.get("maxRetries");
    }
    
    public long getInitialDelayMs() {
        return (long) config.get("initialDelayMs");
    }
    
    public double getRetryFactor() {
        return (double) config.get("retryFactor");
    }
    
    public String getBackoffMode() {
        return (String) config.get("backoffMode");
    }
    
    public boolean isEnableJitter() {
        return (boolean) config.get("enableJitter");
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String url;
        private String eventType;
        private int maxRetries = 3;
        private long initialDelayMs = 100;
        private double retryFactor = 1.0;
        private String backoffMode = "LINEAR";
        private boolean enableJitter = false;
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder initialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
            return this;
        }
        
        public Builder retryFactor(double retryFactor) {
            this.retryFactor = retryFactor;
            return this;
        }
        
        public Builder backoffMode(String backoffMode) {
            this.backoffMode = backoffMode;
            return this;
        }
        
        public Builder enableJitter(boolean enableJitter) {
            this.enableJitter = enableJitter;
            return this;
        }
        
        public WebhookConfig build() {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("URL is required");
            }
            if (eventType == null || eventType.isEmpty()) {
                throw new IllegalArgumentException("Event type is required");
            }
            return new WebhookConfig(this);
        }
    }
}
