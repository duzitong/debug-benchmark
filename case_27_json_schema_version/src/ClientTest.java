/**
 * Test program to validate user profile retrieval from the API.
 * Verifies that all expected fields are present in the response.
 */
public class ClientTest {
    
    public static void main(String[] args) {
        String serverUrl = "http://localhost:8080";
        if (args.length > 0) {
            serverUrl = args[0];
        }
        
        System.out.println("Testing User Profile API...");
        System.out.println("Server URL: " + serverUrl);
        
        ApiClient client = new ApiClient(serverUrl);
        
        try {
            // Fetch user profile
            UserProfile profile = client.getUserProfile("user123");
            
            System.out.println("Received profile: " + profile);
            
            // Validate all required fields are present
            boolean hasErrors = false;
            
            if (profile.getId() == null || profile.getId().isEmpty()) {
                System.err.println("ERROR: Missing field 'id'");
                hasErrors = true;
            }
            
            if (profile.getName() == null || profile.getName().isEmpty()) {
                System.err.println("ERROR: Missing field 'name'");
                hasErrors = true;
            }
            
            if (profile.getEmail() == null || profile.getEmail().isEmpty()) {
                System.err.println("ERROR: Missing field 'email'");
                hasErrors = true;
            }
            
            if (profile.getPhone() == null || profile.getPhone().isEmpty()) {
                System.err.println("ERROR: Missing field 'phone'");
                hasErrors = true;
            }
            
            if (profile.getAddress() == null || profile.getAddress().isEmpty()) {
                System.err.println("ERROR: Missing field 'address'");
                hasErrors = true;
            }
            
            if (profile.getDepartment() == null || profile.getDepartment().isEmpty()) {
                System.err.println("ERROR: Missing field 'department'");
                hasErrors = true;
            }
            
            if (profile.getRole() == null || profile.getRole().isEmpty()) {
                System.err.println("ERROR: Missing field 'role'");
                hasErrors = true;
            }
            
            if (hasErrors) {
                System.err.println("FAILED: User profile is incomplete");
                System.exit(1);
            }
            
            System.out.println("SUCCESS: All fields present");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
