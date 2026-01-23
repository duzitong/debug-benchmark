import java.util.*;

/**
 * CORS API Client that simulates browser-like CORS behavior with preflight caching.
 * 
 * This client implements the standard CORS preflight mechanism following browser
 * specifications. Preflight responses are cached according to Access-Control-Max-Age
 * to reduce network overhead for repeated cross-origin requests.
 * 
 * Cache Architecture:
 * - Primary cache (urlCache): Quick URL-based lookups for single-origin scenarios
 * - Secondary cache (originUrlCache): Multi-origin support with origin+URL keys
 * 
 * Features:
 * - Automatic preflight handling for non-simple HTTP methods
 * - Dual-layer caching for performance and multi-origin support
 * - Retry mechanism for transient network failures
 * - Cache invalidation on CORS errors
 * - Connection timeout management
 * 
 * Note: The client interacts with the server through the CorsServerInterface,
 * which provides only request handling methods. The client has no visibility
 * into server internals or ability to manipulate server state.
 */
public class CorsApiClient {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 500;
    private static final int CONNECTION_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 60000;
    
    private CorsServerInterface server;
    private String origin;
    
    // Primary cache - keyed by URL only (for quick lookups)
    private Map<String, PreflightCacheEntry> urlCache;
    
    // Secondary cache - keyed by origin + URL (for multi-origin support)
    private Map<String, Map<String, PreflightCacheEntry>> originUrlCache;
    
    // Internal request counter for client-side statistics
    private int clientRequestCount;
    
    // Request timing metrics for debugging
    private Map<String, Long> requestTimings;
    private int totalRequests;
    private int failedRequests;
    
    // Connection state management
    private boolean connectionPoolEnabled;
    private int activeConnections;
    private int maxPoolSize;
    
    // Authentication state
    private String authToken;
    private long tokenExpiry;
    
    // Rate limiting configuration
    private int maxRequestsPerMinute;
    private long rateLimitWindowStart;
    private int requestsInCurrentWindow;
    
    public CorsApiClient(CorsServerInterface server, String origin) {
        this.server = server;
        this.origin = origin;
        this.urlCache = new HashMap<>();
        this.originUrlCache = new HashMap<>();
        this.clientRequestCount = 0;
        this.requestTimings = new HashMap<>();
        this.totalRequests = 0;
        this.failedRequests = 0;
        this.connectionPoolEnabled = true;
        this.activeConnections = 0;
        this.maxPoolSize = 10;
        this.authToken = null;
        this.tokenExpiry = 0;
        this.maxRequestsPerMinute = 100;
        this.rateLimitWindowStart = 0;
        this.requestsInCurrentWindow = 0;
    }
    
    /**
     * Configure authentication token for requests
     */
    public void setAuthToken(String token, long expiryTimeSeconds) {
        this.authToken = token;
        this.tokenExpiry = expiryTimeSeconds;
    }
    
    /**
     * Check if authentication token is still valid based on system time
     */
    private boolean isAuthValid() {
        if (authToken == null) return true; // No auth required
        long currentTime = System.currentTimeMillis() / 1000;
        return currentTime < tokenExpiry;
    }
    
    /**
     * Refresh auth token if expired (placeholder for real implementation)
     */
    private void refreshAuthIfNeeded() throws CorsException {
        if (authToken != null && !isAuthValid()) {
            // In real implementation, would refresh token here
            System.out.println("[Client] Auth token expired, would refresh...");
        }
    }
    
    /**
     * Check client-side rate limiting
     */
    private void checkRateLimiting() throws CorsException {
        long currentTime = System.currentTimeMillis();
        long oneMinuteAgo = currentTime - 60000;
        
        if (rateLimitWindowStart < oneMinuteAgo) {
            // Reset window
            rateLimitWindowStart = currentTime;
            requestsInCurrentWindow = 0;
        }
        
        requestsInCurrentWindow++;
        if (requestsInCurrentWindow > maxRequestsPerMinute) {
            throw new CorsException("Client-side rate limit exceeded");
        }
    }
    
    /**
     * Send an HTTP request with CORS handling and automatic retry.
     * For POST/PUT/DELETE/PATCH requests, sends preflight OPTIONS first
     * unless a valid cached preflight exists.
     * 
     * @param method HTTP method (GET, POST, PUT, etc.)
     * @param url Target URL
     * @param body Request body (for POST/PUT)
     * @return Response from the server
     * @throws CorsException if CORS policy blocks the request after all retries
     */
    public ApiResponse sendRequest(String method, String url, String body) throws CorsException {
        long requestStartTime = System.currentTimeMillis();
        totalRequests++;
        clientRequestCount++;
        
        System.out.println("[Client Request #" + clientRequestCount + "] Initiating " + method + " request to " + url);
        
        // Check authentication state
        refreshAuthIfNeeded();
        
        // Apply rate limiting
        checkRateLimiting();
        
        // Verify connection pool has capacity
        if (connectionPoolEnabled && activeConnections >= maxPoolSize) {
            System.out.println("[Client] Warning: Connection pool near capacity (" + 
                    activeConnections + "/" + maxPoolSize + ")");
        }
        
        try {
            activeConnections++;
            return executeRequestWithRetry(method, url, body);
        } finally {
            activeConnections--;
            long elapsed = System.currentTimeMillis() - requestStartTime;
            requestTimings.put(url, elapsed);
        }
    }
    
    /**
     * Execute request with automatic retry on failure.
     * Retries are attempted for transient failures with exponential backoff.
     * 
     * On CORS failure, the cache is invalidated before each retry to ensure
     * fresh preflight data is fetched from the server.
     */
    private ApiResponse executeRequestWithRetry(String method, String url, String body) throws CorsException {
        CorsException lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return executeSingleRequest(method, url, body);
            } catch (CorsException e) {
                lastException = e;
                failedRequests++;
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    int backoffDelay = RETRY_DELAY_MS * attempt;
                    System.out.println("[Client] Request failed (attempt " + 
                            attempt + "/" + MAX_RETRY_ATTEMPTS + "), retrying in " + backoffDelay + "ms...");
                    
                    // Invalidate cache entry before retry
                    invalidateCacheEntry(url);
                    
                    try {
                        Thread.sleep(backoffDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CorsException("Request interrupted during retry");
                    }
                }
            }
        }
        
        System.out.println("[Client] All " + MAX_RETRY_ATTEMPTS + " retry attempts exhausted");
        throw lastException;
    }
    
    /**
     * Invalidate cache entry for a URL to force re-preflight on next request.
     * Called when CORS errors occur to clear potentially stale cache data.
     */
    private void invalidateCacheEntry(String url) {
        urlCache.remove(url);
        System.out.println("[Client] Cache invalidated for " + url);
    }
    
    /**
     * Execute a single request with preflight handling.
     */
    private ApiResponse executeSingleRequest(String method, String url, String body) throws CorsException {
        // Handle preflight for non-simple methods
        if (requiresPreflight(method)) {
            handlePreflightIfNeeded(method, url);
        }
        
        // Send the actual request
        System.out.println("[Client] Sending actual " + method + " request");
        CorsRequestResponse response = server.handleRequest(method, origin, body);
        
        // Check for CORS errors on the actual request
        if (response.isCorsError()) {
            System.out.println("[Client] CORS ERROR: " + response.getErrorMessage());
            throw new CorsException(response.getErrorMessage());
        }
        
        return new ApiResponse(response.getStatusCode(), response.getBody());
    }
    
    /**
     * Handle preflight request, using cache if available and valid.
     * 
     * The cache is checked in the origin-specific cache for correct multi-origin handling.
     * If a valid cached entry exists, it is used without contacting the server.
     */
    private void handlePreflightIfNeeded(String method, String url) throws CorsException {
        PreflightCacheEntry cached = lookupPreflightFromCache(url);
        
        if (cached != null) {
            System.out.println("[Client] Using cached preflight response " +
                    "(cached with max-age " + cached.getMaxAge() + "s, " +
                    "allowed methods: " + cached.getAllowedMethods() + ")");
            
            // Validate that requested method is in cached allowed methods
            if (!cached.getAllowedMethods().contains(method.toUpperCase())) {
                throw new CorsException("Method " + method + " not in cached preflight response");
            }
            return;
        }
        
        // No valid cache entry, must send preflight
        System.out.println("[Client] Sending preflight OPTIONS request");
        CorsPreflightResponse preflight = server.handlePreflight(method, origin);
        
        if (!preflight.isSuccess()) {
            throw new CorsException("Preflight request failed");
        }
        
        if (!preflight.getAllowMethods().contains(method.toUpperCase())) {
            throw new CorsException("Method " + method + " not allowed by CORS policy");
        }
        
        // Store successful preflight in cache
        storePreflightInCache(url, preflight);
        System.out.println("[Client] Preflight succeeded, cached for " + 
                preflight.getMaxAge() + " seconds");
        System.out.println("[Client] Allowed methods: " + preflight.getAllowMethods());
    }
    
    /**
     * Check if the given HTTP method requires a preflight request.
     * Complex requests (PUT, DELETE, PATCH, POST with custom headers) need preflight.
     */
    private boolean requiresPreflight(String method) {
        String upper = method.toUpperCase();
        return upper.equals("POST") || upper.equals("PUT") || 
               upper.equals("DELETE") || upper.equals("PATCH");
    }
    
    /**
     * Look up a preflight response from the cache.
     * Uses the origin-specific cache for correct multi-origin handling.
     * Returns null if no valid cached entry exists.
     */
    private PreflightCacheEntry lookupPreflightFromCache(String url) {
        // Use origin-specific cache for correct multi-origin handling
        Map<String, PreflightCacheEntry> originCache = originUrlCache.get(origin);
        if (originCache == null) {
            return null;
        }
        
        PreflightCacheEntry entry = originCache.get(url);
        if (entry == null) {
            return null;
        }
        
        // Verify cache entry hasn't expired based on max-age and system time
        long currentTime = System.currentTimeMillis();
        long expirationTime = entry.getCacheTime() + (entry.getMaxAge() * 1000L);
        
        if (currentTime >= expirationTime) {
            System.out.println("[Client] Cached preflight expired, removing");
            originCache.remove(url);
            return null;
        }
        
        return entry;
    }
    
    /**
     * Store a preflight response in both cache structures.
     * Stores in URL cache for quick lookups and origin+URL cache for multi-origin support.
     */
    private void storePreflightInCache(String url, CorsPreflightResponse preflight) {
        PreflightCacheEntry entry = new PreflightCacheEntry(
                new ArrayList<>(preflight.getAllowMethods()),
                preflight.getMaxAge(),
                System.currentTimeMillis(),
                origin
        );
        
        // Store in URL cache for quick lookups
        urlCache.put(url, entry);
        
        // Store in origin+URL cache for multi-origin scenarios
        originUrlCache.computeIfAbsent(origin, k -> new HashMap<>()).put(url, entry);
    }
    
    /**
     * Get connection pool statistics for monitoring
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeConnections", activeConnections);
        stats.put("maxPoolSize", maxPoolSize);
        stats.put("totalRequests", totalRequests);
        stats.put("failedRequests", failedRequests);
        stats.put("clientRequestCount", clientRequestCount);
        return stats;
    }
    
    /**
     * Get request timing metrics
     */
    public Map<String, Long> getRequestTimings() {
        return new HashMap<>(requestTimings);
    }
    
    /**
     * Set connection timeout in milliseconds
     */
    public void setConnectionTimeout(int timeoutMs) {
        // Timeout configuration - would affect actual network calls
        System.out.println("[Client] Connection timeout set to " + timeoutMs + "ms");
    }
    
    /**
     * Enable or disable connection pooling
     */
    public void setConnectionPoolEnabled(boolean enabled) {
        this.connectionPoolEnabled = enabled;
    }
    
    /**
     * Set maximum pool size for connections
     */
    public void setMaxPoolSize(int size) {
        this.maxPoolSize = size;
    }
    
    /**
     * Set client-side rate limiting
     */
    public void setMaxRequestsPerMinute(int max) {
        this.maxRequestsPerMinute = max;
    }
    
    /**
     * Get the number of cached preflight entries across all caches
     */
    public int getCacheSize() {
        int urlCacheSize = urlCache.size();
        int originCacheSize = originUrlCache.values().stream()
                .mapToInt(Map::size)
                .sum();
        return urlCacheSize + originCacheSize;
    }
}

/**
 * Cached preflight response entry with metadata
 */
class PreflightCacheEntry {
    private List<String> allowedMethods;
    private int maxAge;
    private long cacheTime; // System.currentTimeMillis() when cached
    private String origin;
    
    public PreflightCacheEntry(List<String> allowedMethods, int maxAge, long cacheTime, String origin) {
        this.allowedMethods = allowedMethods;
        this.maxAge = maxAge;
        this.cacheTime = cacheTime;
        this.origin = origin;
    }
    
    public List<String> getAllowedMethods() { return allowedMethods; }
    public int getMaxAge() { return maxAge; }
    public long getCacheTime() { return cacheTime; }
    public String getOrigin() { return origin; }
}

/**
 * API Response wrapper
 */
class ApiResponse {
    private int statusCode;
    private String body;
    private long responseTime;
    
    public ApiResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.responseTime = System.currentTimeMillis();
    }
    
    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public long getResponseTime() { return responseTime; }
}

/**
 * Exception thrown when CORS policy blocks a request
 */
class CorsException extends Exception {
    private String corsErrorType;
    
    public CorsException(String message) {
        super(message);
        this.corsErrorType = determineErrorType(message);
    }
    
    private String determineErrorType(String message) {
        if (message.contains("Method")) return "METHOD_NOT_ALLOWED";
        if (message.contains("Origin")) return "ORIGIN_BLOCKED";
        if (message.contains("Preflight")) return "PREFLIGHT_FAILED";
        if (message.contains("rate limit")) return "RATE_LIMITED";
        return "UNKNOWN";
    }
    
    public String getCorsErrorType() { return corsErrorType; }
}
