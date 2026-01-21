import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test program to demonstrate pagination client behavior.
 * Fetches all items and validates the results.
 */
public class ClientTest {
    
    public static void main(String[] args) {
        System.out.println("=== Pagination Client Test ===\n");
        
        try {
            // Create server and client
            WeightedItemServer server = new WeightedItemServer();
            PaginationClient client = new PaginationClient(server, 10);
            
            System.out.println("Server Configuration:");
            System.out.println("  Total items: " + server.getTotalItemCount());
            System.out.println("  Total weighted size: " + server.getTotalWeightedSize());
            System.out.println("  Client batch size: 10\n");
            
            // Fetch all items
            System.out.println("Fetching all items...\n");
            List<Item> fetchedItems = client.fetchAllItems();
            
            // Display results
            System.out.println("Fetched Items:");
            for (Item item : fetchedItems) {
                System.out.println("  " + item);
            }
            System.out.println();
            
            // Check for duplicates
            Set<String> seenIds = new HashSet<>();
            int duplicateCount = 0;
            for (Item item : fetchedItems) {
                if (!seenIds.add(item.getId())) {
                    duplicateCount++;
                    System.out.println("WARNING: Duplicate item found: " + item.getId());
                }
            }
            
            // Validate completeness
            int expectedCount = client.getExpectedTotalCount();
            int actualCount = fetchedItems.size();
            int uniqueCount = seenIds.size();
            
            System.out.println("=== Results ===");
            System.out.println("Expected items: " + expectedCount);
            System.out.println("Fetched items:  " + actualCount);
            System.out.println("Unique items:   " + uniqueCount);
            System.out.println("Duplicates:     " + duplicateCount);
            
            // Determine test outcome
            boolean passed = client.validateCompleteness(fetchedItems);
            
            if (passed && duplicateCount == 0) {
                System.out.println("\n[PASS] All items fetched successfully!");
                System.exit(0);
            } else {
                System.out.println("\n[FAIL] Pagination issue detected!");
                if (actualCount < expectedCount) {
                    System.out.println("  Missing " + (expectedCount - actualCount) + " items");
                    System.out.println("  Some items were skipped during pagination");
                }
                if (duplicateCount > 0) {
                    System.out.println("  Found " + duplicateCount + " duplicate items");
                }
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
