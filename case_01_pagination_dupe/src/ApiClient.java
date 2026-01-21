import java.util.ArrayList;
import java.util.List;

/**
 * Client that fetches all items from a paginated API.
 */
public class ApiClient {
    private final PaginatedServer server;
    private static final int PAGE_SIZE = 10;

    public ApiClient(PaginatedServer server) {
        this.server = server;
    }

    /**
     * Fetches a single page of items from the server.
     */
    public List<Item> fetchPage(int offset, int limit) {
        return server.fetchPage(offset, limit);
    }

    /**
     * Fetches all items by paginating through the API.
     * Uses offset-based pagination.
     */
    public List<Item> fetchAllItems() {
        List<Item> allItems = new ArrayList<>();
        int offset = 0;
        
        while (true) {
            List<Item> page = fetchPage(offset, PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            allItems.addAll(page);
            offset += PAGE_SIZE;
        }
        
        return allItems;
    }
}
