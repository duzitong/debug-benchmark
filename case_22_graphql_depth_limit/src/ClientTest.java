/**
 * Test program for the GraphQL client.
 * Fetches organization hierarchy and validates the response structure.
 */
public class ClientTest {

    public static void main(String[] args) {
        System.out.println("Testing GraphQL Organization Hierarchy Query");
        System.out.println("============================================");
        
        GraphQLClient client = new GraphQLClient("http://localhost:8080/graphql");
        
        try {
            // Fetch the organization hierarchy
            String response = client.fetchOrganizationHierarchy();
            System.out.println("Response received from server");
            
            // Validate that all expected nested levels are present
            boolean valid = validateNestedStructure(client, response);
            
            if (valid) {
                System.out.println("SUCCESS: All nested levels present in response");
                System.exit(0);
            } else {
                System.out.println("FAILURE: Some nested levels are missing from response");
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Validates that the response contains all expected nested levels.
     * Checks for presence of data at each hierarchy level.
     */
    private static boolean validateNestedStructure(GraphQLClient client, String response) {
        System.out.println("\nValidating nested structure...");
        
        // Check for organization level
        if (!response.contains("\"organization\"")) {
            System.out.println("  - Missing: organization");
            return false;
        }
        System.out.println("  - Found: organization");
        
        // Check for departments level
        if (!response.contains("\"departments\"")) {
            System.out.println("  - Missing: departments");
            return false;
        }
        System.out.println("  - Found: departments");
        
        // Check for teams level
        if (!response.contains("\"teams\"")) {
            System.out.println("  - Missing: teams");
            return false;
        }
        System.out.println("  - Found: teams");
        
        // Check for members level
        if (!response.contains("\"members\"")) {
            System.out.println("  - Missing: members");
            return false;
        }
        System.out.println("  - Found: members");
        
        // Check for projects level
        if (!response.contains("\"projects\"")) {
            System.out.println("  - Missing: projects");
            return false;
        }
        System.out.println("  - Found: projects");
        
        // Check for tasks level - should have actual data
        String tasks = client.extractTasks(response);
        if (tasks == null || tasks.equals("null")) {
            System.out.println("  - Missing: tasks (received null)");
            return false;
        }
        System.out.println("  - Found: tasks");
        
        // Check for subtasks level - should have actual data
        String subtasks = client.extractSubtasks(response);
        if (subtasks == null || subtasks.equals("null")) {
            System.out.println("  - Missing: subtasks (received null)");
            return false;
        }
        System.out.println("  - Found: subtasks");
        
        // Check for comments at the deepest level
        String comment = client.extractDeepestComment(response);
        if (comment == null) {
            System.out.println("  - Missing: comments (received null)");
            return false;
        }
        System.out.println("  - Found: comments with text \"" + comment + "\"");
        
        return true;
    }
}
