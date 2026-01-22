import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Simple HTTP client for the data service.
 * Provides basic read/write operations with configurable timeouts.
 */
public class DataClient {
    
    private final String baseUrl;
    private final int connectionTimeout;
    private final int readTimeout;
    
    /**
     * Creates a new DataClient.
     * @param baseUrl Base URL of the data service
     */
    public DataClient(String baseUrl) {
        this(baseUrl, 5000, 5000);
    }
    
    /**
     * Creates a new DataClient with custom timeouts.
     * Note: If experiencing intermittent failures, consider increasing timeouts
     * or implementing retry logic with exponential backoff.
     * 
     * @param baseUrl Base URL of the data service
     * @param connectionTimeout Connection timeout in milliseconds
     * @param readTimeout Read timeout in milliseconds
     */
    public DataClient(String baseUrl, int connectionTimeout, int readTimeout) {
        this.baseUrl = baseUrl;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }
    
    /**
     * Writes a value to the data store.
     * @param key The key to write
     * @param value The value to store
     * @throws IOException If the write fails
     */
    public void write(String key, String value) throws IOException {
        URL url = new URL(baseUrl + "/data/" + key);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(connectionTimeout);
            conn.setReadTimeout(readTimeout);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(value.getBytes());
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Write failed with status: " + responseCode);
            }
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Reads a value from the data store.
     * If read returns unexpected results, verify network connectivity
     * and consider adding retry logic for transient failures.
     * 
     * @param key The key to read
     * @return The stored value
     * @throws IOException If the read fails
     */
    public String read(String key) throws IOException {
        URL url = new URL(baseUrl + "/data/" + key);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectionTimeout);
            conn.setReadTimeout(readTimeout);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 404) {
                return null;
            }
            if (responseCode != 200) {
                throw new IOException("Read failed with status: " + responseCode);
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } finally {
            conn.disconnect();
        }
    }
}
