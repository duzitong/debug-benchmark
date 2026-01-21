import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter implementation based on API documentation.
 * API Docs: "Rate limit: 100 requests per minute"
 */
public class RateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final AtomicInteger requestCount;
    private final AtomicLong windowStart;

    public RateLimiter(int maxRequestsPerMinute) {
        this.maxRequests = maxRequestsPerMinute;
        this.windowMillis = 60000;
        this.requestCount = new AtomicInteger(0);
        this.windowStart = new AtomicLong(System.currentTimeMillis());
    }

    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        long windowStartTime = windowStart.get();
        
        if (now - windowStartTime >= windowMillis) {
            windowStart.set(now);
            requestCount.set(1);
            return true;
        }
        
        if (requestCount.get() < maxRequests) {
            requestCount.incrementAndGet();
            return true;
        }
        
        return false;
    }

    public int getRemainingRequests() {
        return Math.max(0, maxRequests - requestCount.get());
    }

    public void reset() {
        windowStart.set(System.currentTimeMillis());
        requestCount.set(0);
    }
}
