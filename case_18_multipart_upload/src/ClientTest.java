import java.util.Arrays;
import java.util.UUID;

public class ClientTest {
    
    private static final int CHUNK_SIZE = 1024 * 1024;
    
    public static void main(String[] args) {
        String serverUrl = "http://localhost:8080";
        if (args.length > 0) {
            serverUrl = args[0];
        }
        
        FileUploadClient client = new FileUploadClient(serverUrl);
        
        try {
            byte[] testData = createTestData(3);
            String fileId = "test-" + UUID.randomUUID().toString();
            
            System.out.println("Uploading file with " + (testData.length / CHUNK_SIZE) + " chunks...");
            client.uploadFile(fileId, testData);
            System.out.println("Upload complete.");
            
            System.out.println("Downloading file...");
            byte[] downloadedData = client.downloadFile(fileId);
            System.out.println("Download complete. Size: " + downloadedData.length + " bytes");
            
            if (Arrays.equals(testData, downloadedData)) {
                System.out.println("SUCCESS: Downloaded file matches original!");
            } else {
                System.out.println("ERROR: Downloaded file does not match original!");
                printDataComparison(testData, downloadedData);
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            client.shutdown();
        }
    }
    
    private static byte[] createTestData(int numChunks) {
        byte[] data = new byte[numChunks * CHUNK_SIZE];
        
        for (int chunk = 0; chunk < numChunks; chunk++) {
            int startOffset = chunk * CHUNK_SIZE;
            byte pattern = (byte) ('A' + chunk);
            
            for (int i = 0; i < CHUNK_SIZE; i++) {
                data[startOffset + i] = pattern;
            }
        }
        
        return data;
    }
    
    private static void printDataComparison(byte[] expected, byte[] actual) {
        System.out.println("\n=== Data Comparison ===");
        System.out.println("Expected size: " + expected.length);
        System.out.println("Actual size: " + actual.length);
        
        int numChunks = expected.length / CHUNK_SIZE;
        System.out.println("\nExpected chunk order:");
        for (int i = 0; i < numChunks; i++) {
            byte pattern = expected[i * CHUNK_SIZE];
            System.out.println("  Chunk " + i + ": pattern '" + (char) pattern + "'");
        }
        
        System.out.println("\nActual chunk order:");
        for (int i = 0; i < numChunks; i++) {
            byte pattern = actual[i * CHUNK_SIZE];
            System.out.println("  Chunk " + i + ": pattern '" + (char) pattern + "'");
        }
    }
}
