import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * REST API client for fetching user profiles from the server.
 * Makes standard HTTP GET requests with JSON content type.
 */
public class ApiClient {
    private final String baseUrl;
    
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /**
     * Fetches a user profile by ID from the API.
     * Uses standard JSON content negotiation to get the user data.
     * 
     * @param userId The ID of the user to fetch
     * @return UserProfile populated with all available fields
     * @throws Exception if the request fails
     */
    public UserProfile getUserProfile(String userId) throws Exception {
        URL url = new URL(baseUrl + "/api/users/" + userId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            // Request JSON response format
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP error: " + responseCode);
            }
            
            // Read the response body
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse JSON response into UserProfile
            return parseUserProfile(response.toString());
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Simple JSON parser for UserProfile.
     * Extracts fields from the JSON response.
     */
    private UserProfile parseUserProfile(String json) {
        UserProfile profile = new UserProfile();
        
        profile.setId(extractJsonField(json, "id"));
        profile.setName(extractJsonField(json, "name"));
        profile.setEmail(extractJsonField(json, "email"));
        profile.setPhone(extractJsonField(json, "phone"));
        profile.setAddress(extractJsonField(json, "address"));
        profile.setDepartment(extractJsonField(json, "department"));
        profile.setRole(extractJsonField(json, "role"));
        
        return profile;
    }
    
    /**
     * Extracts a string field value from JSON.
     */
    private String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) {
            return null;
        }
        
        start += pattern.length();
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        if (start >= json.length()) {
            return null;
        }
        
        // Check for null value
        if (json.substring(start).startsWith("null")) {
            return null;
        }
        
        // Expect quoted string
        if (json.charAt(start) != '"') {
            return null;
        }
        
        start++; // Skip opening quote
        int end = start;
        while (end < json.length() && json.charAt(end) != '"') {
            if (json.charAt(end) == '\\') {
                end++; // Skip escaped character
            }
            end++;
        }
        
        return json.substring(start, end);
    }
}
