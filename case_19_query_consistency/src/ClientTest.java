/**
 * Test client for the data service.
 * Verifies basic read/write functionality.
 */
public class ClientTest {
    
    public static void main(String[] args) {
        String serverUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        
        DataClient client = new DataClient(serverUrl);
        
        try {
            String testKey = "test-key";
            String expectedValue = "UPDATED_VALUE_" + System.currentTimeMillis();
            
            System.out.println("Writing value: " + expectedValue);
            client.write(testKey, expectedValue);
            
            System.out.println("Reading value back...");
            String actualValue = client.read(testKey);
            
            System.out.println("Expected: " + expectedValue);
            System.out.println("Actual:   " + actualValue);
            
            if (expectedValue.equals(actualValue)) {
                System.out.println("SUCCESS: Values match");
                System.exit(0);
            } else {
                System.out.println("FAILURE: Values do not match");
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
