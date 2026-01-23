import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

public class SessionClient {
    
    private final String baseUrl;
    private final CookieManager cookieManager;
    
    public SessionClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.cookieManager = new CookieManager();
        this.cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(this.cookieManager);
    }
    
    public String createSession(String userId) throws IOException {
        URL url = new URL(baseUrl + "/session");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String requestBody = "{\"userId\": \"" + userId + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 201 && responseCode != 200) {
                throw new IOException("Failed to create session: HTTP " + responseCode);
            }
            
            String responseBody = readResponse(conn);
            String sessionId = extractJsonField(responseBody, "sessionId");
            
            return sessionId;
        } finally {
            conn.disconnect();
        }
    }
    
    public Map<String, Object> getSessionData(String sessionId) throws IOException {
        URL url = new URL(baseUrl + "/session/" + sessionId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorBody = readErrorResponse(conn);
                throw new IOException("Failed to get session: HTTP " + responseCode + " - " + errorBody);
            }
            
            String responseBody = readResponse(conn);
            return parseJsonResponse(responseBody);
        } finally {
            conn.disconnect();
        }
    }
    
    public boolean updateSession(String sessionId, String data) throws IOException {
        URL url = new URL(baseUrl + "/session/" + sessionId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String requestBody = "{\"data\": \"" + data + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorBody = readErrorResponse(conn);
                throw new IOException("Failed to update session: HTTP " + responseCode + " - " + errorBody);
            }
            
            return true;
        } finally {
            conn.disconnect();
        }
    }
    
    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    private String readErrorResponse(HttpURLConnection conn) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return "Unable to read error response";
        }
    }
    
    private String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    private Map<String, Object> parseJsonResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("raw", json);
        
        String sessionId = extractJsonField(json, "sessionId");
        if (sessionId != null) {
            result.put("sessionId", sessionId);
        }
        
        String data = extractJsonField(json, "data");
        if (data != null) {
            result.put("data", data);
        }
        
        String userId = extractJsonField(json, "userId");
        if (userId != null) {
            result.put("userId", userId);
        }
        
        return result;
    }
}
