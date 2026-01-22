import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * GraphQL client for querying organization hierarchy data.
 * Provides methods to fetch nested organizational structures.
 */
public class GraphQLClient {
    private final String endpoint;

    public GraphQLClient(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Fetches the complete organization hierarchy including all nested levels.
     * Returns the raw JSON response from the server.
     */
    public String fetchOrganizationHierarchy() throws IOException {
        String query = buildOrganizationQuery();
        return executeQuery(query);
    }

    /**
     * Builds a GraphQL query to fetch the organization hierarchy.
     * The query requests nested data through multiple levels.
     */
    private String buildOrganizationQuery() {
        StringBuilder query = new StringBuilder();
        query.append("query GetOrganization {");
        query.append("  organization {");
        query.append("    name");
        query.append("    departments {");
        query.append("      name");
        query.append("      teams {");
        query.append("        name");
        query.append("        members {");
        query.append("          name");
        query.append("          projects {");
        query.append("            name");
        query.append("            tasks {");
        query.append("              title");
        query.append("              subtasks {");
        query.append("                title");
        query.append("                comments {");
        query.append("                  text");
        query.append("                  author");
        query.append("                }");
        query.append("              }");
        query.append("            }");
        query.append("          }");
        query.append("        }");
        query.append("      }");
        query.append("    }");
        query.append("  }");
        query.append("}");
        return query.toString();
    }

    /**
     * Executes a GraphQL query against the server.
     */
    private String executeQuery(String query) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String requestBody = "{\"query\":\"" + escapeJson(query) + "\"}";
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300) 
            ? conn.getInputStream() 
            : conn.getErrorStream();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    /**
     * Escapes special characters for JSON string.
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Parses the response and extracts nested comment data.
     * Returns the first comment text found at the deepest level.
     */
    public String extractDeepestComment(String jsonResponse) {
        // Navigate through the nested structure to find comments
        String commentsSection = extractJsonValue(jsonResponse, "comments");
        if (commentsSection == null || commentsSection.equals("null")) {
            return null;
        }
        return extractJsonValue(commentsSection, "text");
    }

    /**
     * Extracts subtask data from the response.
     */
    public String extractSubtasks(String jsonResponse) {
        return extractJsonValue(jsonResponse, "subtasks");
    }

    /**
     * Extracts task data from the response.
     */
    public String extractTasks(String jsonResponse) {
        return extractJsonValue(jsonResponse, "tasks");
    }

    /**
     * Simple JSON value extractor for a given key.
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }
        
        int valueStart = keyIndex + searchKey.length();
        char firstChar = json.charAt(valueStart);
        
        if (firstChar == 'n' && json.substring(valueStart).startsWith("null")) {
            return "null";
        }
        
        if (firstChar == '"') {
            int stringEnd = json.indexOf('"', valueStart + 1);
            return json.substring(valueStart + 1, stringEnd);
        }
        
        if (firstChar == '[' || firstChar == '{') {
            int depth = 0;
            int i = valueStart;
            char openBracket = firstChar;
            char closeBracket = (firstChar == '[') ? ']' : '}';
            
            do {
                char c = json.charAt(i);
                if (c == openBracket) depth++;
                else if (c == closeBracket) depth--;
                i++;
            } while (depth > 0 && i < json.length());
            
            return json.substring(valueStart, i);
        }
        
        return null;
    }
}
