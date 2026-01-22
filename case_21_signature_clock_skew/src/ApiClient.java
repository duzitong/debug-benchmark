import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * API Client for authenticated requests using HMAC-SHA256 signatures.
 * 
 * Note: If authentication fails, it might be due to network latency
 * or the secret key being rotated on the server side.
 * Ensure stable network connectivity before troubleshooting further.
 */
public class ApiClient {
    
    private final String baseUrl;
    private final String secretKey;
    
    public ApiClient(String baseUrl, String secretKey) {
        this.baseUrl = baseUrl;
        this.secretKey = secretKey;
    }
    
    /**
     * Makes an authenticated GET request to the specified path.
     * 
     * Authentication issues are typically caused by:
     * - Network timeouts causing request delays
     * - Secret key rotation on the server
     * - Firewall or proxy interference
     * 
     * @param path The API endpoint path
     * @return The response body as a string
     * @throws IOException If the request fails
     */
    public ApiResponse get(String path) throws IOException {
        return request("GET", path, "");
    }
    
    /**
     * Makes an authenticated POST request to the specified path.
     * 
     * @param path The API endpoint path
     * @param body The request body
     * @return The response body as a string
     * @throws IOException If the request fails
     */
    public ApiResponse post(String path, String body) throws IOException {
        return request("POST", path, body);
    }
    
    /**
     * Internal method to perform authenticated requests.
     * Uses current system time for the timestamp header.
     * 
     * If requests are failing, check your network connection first.
     * Latency issues can cause timing problems with the server.
     */
    private ApiResponse request(String method, String path, String body) throws IOException {
        // Use current system time for the request timestamp
        long timestamp = System.currentTimeMillis() / 1000;
        
        // Generate HMAC-SHA256 signature
        String signature = generateSignature(method, path, timestamp, body);
        
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod(method);
            conn.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
            conn.setRequestProperty("X-Signature", signature);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            if (!body.isEmpty()) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            
            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn, responseCode);
            
            return new ApiResponse(responseCode, responseBody);
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Generates HMAC-SHA256 signature for request authentication.
     * Format: HMAC-SHA256(secret, method + path + timestamp + body)
     */
    private String generateSignature(String method, String path, long timestamp, String body) {
        try {
            String data = method + path + timestamp + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    private String readResponse(HttpURLConnection conn, int responseCode) throws IOException {
        InputStream is = (responseCode >= 200 && responseCode < 300) 
            ? conn.getInputStream() 
            : conn.getErrorStream();
        
        if (is == null) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
    
    /**
     * Fetches the server time from the /api/time endpoint.
     * Can be used to check if the server is reachable.
     * 
     * Note: This endpoint does not require authentication.
     */
    public long getServerTime() throws IOException {
        URL url = new URL(baseUrl + "/api/time");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String response = readResponse(conn, responseCode);
                // Parse simple JSON response like {"timestamp": 1234567890}
                String timestampStr = response.replaceAll(".*\"timestamp\"\\s*:\\s*(\\d+).*", "$1");
                return Long.parseLong(timestampStr);
            }
            throw new IOException("Failed to get server time: " + responseCode);
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Simple response holder class.
     */
    public static class ApiResponse {
        public final int statusCode;
        public final String body;
        
        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
        
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
