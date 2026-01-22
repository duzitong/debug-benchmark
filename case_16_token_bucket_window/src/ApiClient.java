import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ApiClient {
    
    private final String baseUrl;
    private final int rateLimitPerMinute;
    private final int initialBurstAllowed;
    private final long minRequestIntervalMs;
    
    private final AtomicInteger requestsThisWindow = new AtomicInteger(0);
    private final AtomicLong windowStartTime = new AtomicLong(0);
    private final AtomicInteger burstUsed = new AtomicInteger(0);
    private final AtomicLong lastRequestTime = new AtomicLong(0);
    
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger rateLimitedCount = new AtomicInteger(0);
    
    public ApiClient(String baseUrl) {
        this(baseUrl, 10, 5, 5000);
    }
    
    public ApiClient(String baseUrl, int rateLimitPerMinute, int initialBurstAllowed, long minRequestIntervalMs) {
        this.baseUrl = baseUrl;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.initialBurstAllowed = initialBurstAllowed;
        this.minRequestIntervalMs = minRequestIntervalMs;
    }
    
    public ApiResponse fetchResource() throws Exception {
        waitForRateLimit();
        return executeRequest();
    }
    
    private synchronized void waitForRateLimit() throws InterruptedException {
        long now = System.currentTimeMillis();
        long windowDurationMs = 60000;
        
        long currentWindowStart = (now / windowDurationMs) * windowDurationMs;
        
        if (windowStartTime.get() != currentWindowStart) {
            windowStartTime.set(currentWindowStart);
            requestsThisWindow.set(0);
            burstUsed.set(0);
        }
        
        if (requestsThisWindow.get() >= rateLimitPerMinute) {
            long sleepTime = currentWindowStart + windowDurationMs - now;
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
                windowStartTime.set(System.currentTimeMillis() / windowDurationMs * windowDurationMs);
                requestsThisWindow.set(0);
                burstUsed.set(0);
            }
        }
        
        if (burstUsed.get() >= initialBurstAllowed) {
            long timeSinceLastRequest = now - lastRequestTime.get();
            if (timeSinceLastRequest < minRequestIntervalMs) {
                Thread.sleep(minRequestIntervalMs - timeSinceLastRequest);
            }
        }
        
        requestsThisWindow.incrementAndGet();
        if (burstUsed.get() < initialBurstAllowed) {
            burstUsed.incrementAndGet();
        }
        lastRequestTime.set(System.currentTimeMillis());
    }
    
    private ApiResponse executeRequest() throws Exception {
        URL url = new URL(baseUrl + "/api/resource");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(responseCode == 200 ? conn.getInputStream() : conn.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        ApiResponse apiResponse = new ApiResponse(responseCode, response.toString());
        
        if (responseCode == 200) {
            successCount.incrementAndGet();
        } else if (responseCode == 429) {
            rateLimitedCount.incrementAndGet();
        }
        
        return apiResponse;
    }
    
    public int getSuccessCount() {
        return successCount.get();
    }
    
    public int getRateLimitedCount() {
        return rateLimitedCount.get();
    }
    
    public void resetStats() {
        successCount.set(0);
        rateLimitedCount.set(0);
    }
    
    public static class ApiResponse {
        private final int statusCode;
        private final String body;
        
        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public String getBody() {
            return body;
        }
        
        public boolean isSuccess() {
            return statusCode == 200;
        }
        
        public boolean isRateLimited() {
            return statusCode == 429;
        }
    }
}
