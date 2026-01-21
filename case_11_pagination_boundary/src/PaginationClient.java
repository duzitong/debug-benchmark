import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client for fetching paginated items from the server.
 * Handles pagination automatically to retrieve all items.
 */
public class PaginationClient {
    private static final Logger logger = Logger.getLogger(PaginationClient.class.getName());
    
    private final WeightedItemServer server;
    private final int batchSize;
    private final int maxRetries;

    public PaginationClient(WeightedItemServer server, int batchSize) {
        this.server = server;
        this.batchSize = batchSize;
        this.maxRetries = 3;
    }

    /**
     * Fetches all items from the server using offset-based pagination.
     * 
     * @return List of all items retrieved from the server
     * @throws Exception if fetching fails after retries
     */
    public List<Item> fetchAllItems() throws Exception {
        List<Item> allItems = new ArrayList<>();
        int offset = 0;
        int consecutiveEmptyPages = 0;
        
        logger.info("Starting to fetch all items with batch size: " + batchSize);
        
        while (true) {
            PageResponse response = fetchPageWithRetry(offset);
            
            if (response == null) {
                throw new Exception("Failed to fetch page after " + maxRetries + " retries");
            }
            
            List<Item> pageItems = response.getItems();
            
            // Handle empty page
            if (pageItems.isEmpty()) {
                consecutiveEmptyPages++;
                if (consecutiveEmptyPages >= 2) {
                    logger.info("Multiple consecutive empty pages, stopping pagination");
                    break;
                }
                offset += batchSize;
                continue;
            }
            
            consecutiveEmptyPages = 0;
            allItems.addAll(pageItems);
            
            logger.fine("Fetched " + pageItems.size() + " items at offset " + offset);
            
            // Check if we've reached the end
            if (!response.hasMore()) {
                logger.info("Server indicates no more items available");
                break;
            }
            
            // Advance offset for next batch
            // Note: pageToken is available in response for tracking purposes
            offset += batchSize;
        }
        
        logger.info("Completed fetching. Total items retrieved: " + allItems.size());
        return allItems;
    }

    /**
     * Fetches a single page with retry logic for resilience.
     */
    private PageResponse fetchPageWithRetry(int offset) {
        int attempts = 0;
        Exception lastError = null;
        
        while (attempts < maxRetries) {
            try {
                return server.getItems(offset, batchSize);
            } catch (Exception e) {
                lastError = e;
                attempts++;
                logger.warning("Fetch attempt " + attempts + " failed: " + e.getMessage());
                
                // Exponential backoff
                try {
                    Thread.sleep((long) Math.pow(2, attempts) * 100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        if (lastError != null) {
            logger.severe("All retry attempts failed: " + lastError.getMessage());
        }
        return null;
    }

    /**
     * Gets the expected total count from the server.
     * This is useful for validation purposes.
     */
    public int getExpectedTotalCount() {
        return server.getTotalItemCount();
    }

    /**
     * Validates that all items were fetched correctly.
     * 
     * @param items The items that were fetched
     * @return true if count matches expected, false otherwise
     */
    public boolean validateCompleteness(List<Item> items) {
        int expected = getExpectedTotalCount();
        int actual = items.size();
        
        if (actual != expected) {
            logger.warning("Item count mismatch! Expected: " + expected + ", Got: " + actual);
            return false;
        }
        
        logger.info("Validation passed: " + actual + " items fetched");
        return true;
    }
}
