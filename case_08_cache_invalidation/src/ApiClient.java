import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Standard API client for profile operations.
 * Uses conventional HTTP methods for CRUD operations.
 */
public class ApiClient {
    private final String baseUrl;
    
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /**
     * Creates a new profile.
     */
    public ProfileData createProfile(String userId, String name, String email) throws IOException {
        String url = baseUrl + "/profile/" + userId;
        String json = String.format(
            "{\"userId\":\"%s\",\"name\":\"%s\",\"email\":\"%s\"}",
            userId, name, email
        );
        
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 201 && responseCode != 200) {
            throw new IOException("Failed to create profile: HTTP " + responseCode);
        }
        
        return parseResponse(conn);
    }
    
    /**
     * Gets a profile by user ID.
     */
    public ProfileData getProfile(String userId) throws IOException {
        String url = baseUrl + "/profile/" + userId;
        
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 404) {
            return null;
        }
        if (responseCode != 200) {
            throw new IOException("Failed to get profile: HTTP " + responseCode);
        }
        
        return parseResponse(conn);
    }
    
    /**
     * Updates an existing profile.
     */
    public ProfileData updateProfile(String userId, String name, String email) throws IOException {
        String url = baseUrl + "/profile/" + userId;
        String json = String.format(
            "{\"name\":\"%s\",\"email\":\"%s\"}",
            name, email
        );
        
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to update profile: HTTP " + responseCode);
        }
        
        return parseResponse(conn);
    }
    
    private ProfileData parseResponse(HttpURLConnection conn) throws IOException {
        String json;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            json = sb.toString();
        }
        
        return fromJson(json);
    }
    
    private ProfileData fromJson(String json) {
        ProfileData p = new ProfileData();
        
        p.setUserId(extractJsonField(json, "userId"));
        p.setName(extractJsonField(json, "name"));
        p.setEmail(extractJsonField(json, "email"));
        
        String versionStr = extractJsonField(json, "version");
        if (versionStr != null && !versionStr.isEmpty()) {
            p.setVersion(Integer.parseInt(versionStr));
        }
        
        String lastModifiedStr = extractJsonField(json, "lastModified");
        if (lastModifiedStr != null && !lastModifiedStr.isEmpty()) {
            p.setLastModified(Long.parseLong(lastModifiedStr));
        }
        
        return p;
    }
    
    private String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        
        int start = idx + pattern.length();
        if (start >= json.length()) return null;
        
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf('"', start);
            if (end == -1) return null;
            return json.substring(start, end);
        } else {
            int end = start;
            while (end < json.length() && 
                   (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
                end++;
            }
            return json.substring(start, end);
        }
    }
}
