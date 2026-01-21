/**
 * Test client for validating binary data streaming.
 */
public class ClientTest {
    
    private static final int TEST_PORT = 19876;
    private static final int REQUESTED_BYTES = 2000;
    
    public static void main(String[] args) {
        StreamServer server = null;
        
        try {
            server = new StreamServer(TEST_PORT);
            server.start();
            
            Thread.sleep(200);
            
            StreamDecoder decoder = new StreamDecoder("localhost", TEST_PORT);
            
            System.out.println("Requesting " + REQUESTED_BYTES + " bytes from server...");
            byte[] data = decoder.fetchData(REQUESTED_BYTES);
            
            System.out.println("Received " + data.length + " bytes");
            
            if (data.length > 0) {
                System.out.println("First 16 bytes (hex): " + bytesToHex(data, 16));
            }
            
            boolean valid = StreamDecoder.validatePattern(data, REQUESTED_BYTES);
            
            if (valid) {
                System.out.println("SUCCESS: Data pattern validated correctly");
                System.exit(0);
            } else {
                System.out.println("FAILURE: Data validation failed");
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }
    
    private static String bytesToHex(byte[] bytes, int limit) {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(bytes.length, limit);
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        return sb.toString().trim();
    }
}
