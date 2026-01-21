import java.util.*;
import java.util.stream.Collectors;

/**
 * Test that validates the pagination client correctly fetches ALL expected items.
 */
public class ClientTest {

    public static void main(String[] args) {
        System.out.println("=== Pagination Test ===\n");
        
        boolean passed = runValidation();
        
        if (passed) {
            System.out.println("\nSUCCESS: All validations passed");
            System.exit(0);
        } else {
            System.out.println("\nFAILURE: Validation failed");
            System.exit(1);
        }
    }
    
    private static boolean runValidation() {
        PaginatedServer server = new PaginatedServer();
        ApiClient client = new ApiClient(server);
        
        List<Item> items = client.fetchAllItems();
        
        System.out.println("Total items fetched: " + items.size());
        
        // Check for expected items 1-32
        Set<Integer> expectedIds = new HashSet<>();
        for (int i = 1; i <= 32; i++) {
            expectedIds.add(i);
        }
        
        Set<Integer> fetchedIds = items.stream()
            .map(Item::getId)
            .collect(Collectors.toSet());
        
        // Find duplicates
        Map<Integer, Long> idCounts = items.stream()
            .collect(Collectors.groupingBy(Item::getId, Collectors.counting()));
        
        List<Integer> duplicates = idCounts.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
        
        // Find missing items
        Set<Integer> missing = new HashSet<>(expectedIds);
        missing.removeAll(fetchedIds);
        
        System.out.println("Expected IDs: 1-32 (32 items)");
        System.out.println("Unique IDs fetched: " + fetchedIds.size());
        
        if (!duplicates.isEmpty()) {
            System.out.println("ERROR: Duplicates found: " + duplicates);
        }
        
        if (!missing.isEmpty()) {
            List<Integer> missingSorted = new ArrayList<>(missing);
            Collections.sort(missingSorted);
            System.out.println("ERROR: Missing items: " + missingSorted);
        }
        
        return duplicates.isEmpty() && missing.isEmpty();
    }

}
