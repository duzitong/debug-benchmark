import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Product Catalog Client
 * 
 * Communicates with the ProductServer to fetch product descriptions.
 * 
 * BUG: Assumes all server responses are UTF-8 encoded.
 * In reality, some products come from a legacy Windows ERP system
 * that uses Windows-1252 encoding. When Windows-1252 bytes are
 * decoded as UTF-8, special characters like ™ (0x99) and € (0x80)
 * become invalid or replacement characters.
 */
public class CatalogClient {
    
    private final String baseUrl;
    
    public CatalogClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /**
     * Fetches a product description by ID.
     * 
     * @param productId The product ID (e.g., "P001")
     * @return The product description
     * @throws IOException if the request fails
     */
    public String getProductDescription(String productId) throws IOException {
        URL url = new URL(baseUrl + "/product/" + productId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Server returned error: " + responseCode);
        }
        
        // BUG: Always decode as UTF-8, ignoring actual encoding
        // The server doesn't specify charset, and some responses are Windows-1252
        try (InputStream is = conn.getInputStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    /**
     * Fetches the list of all product IDs.
     * 
     * @return Array of product IDs
     * @throws IOException if the request fails
     */
    public String[] getProductIds() throws IOException {
        URL url = new URL(baseUrl + "/products");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Server returned error: " + responseCode);
        }
        
        try (InputStream is = conn.getInputStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    response.append(line).append("\n");
                }
            }
            return response.toString().trim().split("\n");
        }
    }
}
