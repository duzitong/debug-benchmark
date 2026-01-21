/**
 * API Client for interacting with the service.
 * 
 * According to the API documentation:
 * "Rate limit: 100 requests per minute"
 * 
 * This client implements rate limiting to stay within the documented limits
 * and avoid 429 responses from the server.
 */
public class ApiClient {
    private final MockServer server;
    private final RateLimiter rateLimiter;

    public ApiClient(MockServer server) {
        this.server = server;
        this.rateLimiter = new RateLimiter(100);
    }

    private ApiResponse executeRequest(String endpoint, String method) {
        if (!rateLimiter.tryAcquire()) {
            throw new RuntimeException("Client-side rate limit exceeded. Wait before making more requests.");
        }
        return server.handleRequest(endpoint, method);
    }

    public ApiResponse getUsers() {
        return executeRequest("/users", "GET");
    }

    public ApiResponse getProducts() {
        return executeRequest("/products", "GET");
    }

    public ApiResponse getOrders() {
        return executeRequest("/orders", "GET");
    }

    public int getRemainingRequests() {
        return rateLimiter.getRemainingRequests();
    }

    public void resetRateLimiter() {
        rateLimiter.reset();
    }
}
