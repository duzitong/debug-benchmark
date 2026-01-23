import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * GrpcClient - Client for calling the gRPC-like service.
 * 
 * This client sets a deadline via the X-Deadline-Ms header, expecting
 * the entire call chain (ServiceA -> ServiceB) to complete within that time.
 * 
 * BUG: The client assumes the server will propagate the deadline to all
 * downstream services. However, ServiceA does NOT propagate the deadline
 * to ServiceB. This means:
 * - Client sets 5-second deadline
 * - ServiceA takes 1 second
 * - ServiceB takes 4 seconds (with its own 30-second default timeout)
 * - Total: 5 seconds, but timing variance causes deadline to be exceeded
 */
public class GrpcClient {
    
    private final String baseUrl;
    private final int connectTimeoutMs;
    
    /**
     * Create a new GrpcClient.
     * 
     * @param host The server host
     * @param port The server port
     */
    public GrpcClient(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
        this.connectTimeoutMs = 1000; // 1 second connect timeout
    }
    
    /**
     * Call the process API with a specified deadline.
     * 
     * The client expects the server to complete the entire operation
     * within the deadline. However, the server does not propagate this
     * deadline to downstream services.
     * 
     * @param requestData The data to process
     * @param deadlineMs The deadline in milliseconds
     * @return The response from the server
     * @throws GrpcException if the request fails or times out
     */
    public GrpcResponse process(String requestData, long deadlineMs) throws GrpcException {
        long startTime = System.currentTimeMillis();
        
        try {
            URL url = new URL(baseUrl + "/api/process");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(connectTimeoutMs);
            // Set read timeout to match the deadline
            conn.setReadTimeout((int) deadlineMs);
            
            // Set the deadline header - client assumes server will propagate this
            conn.setRequestProperty("X-Deadline-Ms", String.valueOf(deadlineMs));
            conn.setRequestProperty("Content-Type", "application/json");
            
            // Send request body
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestData.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            long elapsed = System.currentTimeMillis() - startTime;
            
            // Read response
            String responseBody;
            if (responseCode >= 200 && responseCode < 300) {
                responseBody = readStream(conn.getInputStream());
            } else {
                responseBody = readStream(conn.getErrorStream());
            }
            
            return new GrpcResponse(responseCode, responseBody, elapsed, deadlineMs);
            
        } catch (java.net.SocketTimeoutException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            throw new GrpcException("DEADLINE_EXCEEDED", 
                "Client-side timeout after " + elapsed + "ms (deadline: " + deadlineMs + "ms)", elapsed);
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            throw new GrpcException("UNAVAILABLE", 
                "Connection failed: " + e.getMessage(), elapsed);
        }
    }
    
    /**
     * Call the process API with the default 5-second deadline.
     * 
     * This is the buggy usage pattern - the client assumes 5 seconds is
     * enough for ServiceA (1s) + ServiceB (4s), but due to lack of deadline
     * propagation and timing variance, this often fails.
     */
    public GrpcResponse processWithDefaultDeadline(String requestData) throws GrpcException {
        // BUG: This deadline is exactly ServiceA(1s) + ServiceB(4s) = 5s
        // With timing variance and no deadline propagation, this will fail
        return process(requestData, 5000);
    }
    
    /**
     * Health check endpoint.
     */
    public boolean isHealthy() {
        try {
            URL url = new URL(baseUrl + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }
    
    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        byte[] bytes = is.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Response from the gRPC-like service.
     */
    public static class GrpcResponse {
        public final int statusCode;
        public final String body;
        public final long elapsedMs;
        public final long deadlineMs;
        
        public GrpcResponse(int statusCode, String body, long elapsedMs, long deadlineMs) {
            this.statusCode = statusCode;
            this.body = body;
            this.elapsedMs = elapsedMs;
            this.deadlineMs = deadlineMs;
        }
        
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
        
        public boolean isDeadlineExceeded() {
            return statusCode == 504 || body.contains("DEADLINE_EXCEEDED");
        }
        
        @Override
        public String toString() {
            return "GrpcResponse{statusCode=" + statusCode + 
                   ", elapsed=" + elapsedMs + "ms" +
                   ", deadline=" + deadlineMs + "ms" +
                   ", body=" + body + "}";
        }
    }
    
    /**
     * Exception for gRPC-like errors.
     */
    public static class GrpcException extends Exception {
        public final String status;
        public final long elapsedMs;
        
        public GrpcException(String status, String message, long elapsedMs) {
            super(message);
            this.status = status;
            this.elapsedMs = elapsedMs;
        }
        
        @Override
        public String toString() {
            return "GrpcException{status=" + status + 
                   ", elapsed=" + elapsedMs + "ms" +
                   ", message=" + getMessage() + "}";
        }
    }
}
