import java.io.*;
import java.util.*;

/**
 * Test for CatalogClient
 * 
 * Fetches all products and validates that descriptions contain
 * expected special characters (™, €, —) without corruption.
 * 
 * The bug causes Windows-1252 encoded products to display
 * replacement characters (U+FFFD) or garbled text when
 * decoded incorrectly as UTF-8.
 */
public class ClientTest {
    
    // Unicode replacement character that appears when decoding fails
    private static final char REPLACEMENT_CHAR = '\uFFFD';
    
    // Expected special characters that should appear correctly
    private static final String TRADEMARK = "™";
    private static final String EM_DASH = "—";
    private static final String EURO = "€";
    
    public static void main(String[] args) {
        String serverUrl = "http://localhost:8080";
        if (args.length > 0) {
            serverUrl = args[0];
        }
        
        System.out.println("=== Product Catalog Client Test ===");
        System.out.println("Server: " + serverUrl);
        System.out.println();
        
        CatalogClient client = new CatalogClient(serverUrl);
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        try {
            String[] productIds = client.getProductIds();
            Arrays.sort(productIds);
            
            System.out.println("Found " + productIds.length + " products");
            System.out.println();
            
            for (String productId : productIds) {
                System.out.println("Testing product: " + productId);
                
                try {
                    String description = client.getProductDescription(productId);
                    System.out.println("  Description: " + description);
                    
                    // Check for encoding errors
                    List<String> productErrors = validateDescription(productId, description);
                    
                    if (productErrors.isEmpty()) {
                        System.out.println("  Status: OK");
                        successCount++;
                    } else {
                        System.out.println("  Status: FAILED");
                        for (String error : productErrors) {
                            System.out.println("    - " + error);
                            errors.add(productId + ": " + error);
                        }
                        failureCount++;
                    }
                } catch (IOException e) {
                    System.out.println("  Status: ERROR - " + e.getMessage());
                    errors.add(productId + ": " + e.getMessage());
                    failureCount++;
                }
                
                System.out.println();
            }
            
        } catch (IOException e) {
            System.err.println("Failed to fetch product list: " + e.getMessage());
            System.exit(1);
        }
        
        // Print summary
        System.out.println("=== Test Summary ===");
        System.out.println("Success: " + successCount);
        System.out.println("Failure: " + failureCount);
        
        if (!errors.isEmpty()) {
            System.out.println();
            System.out.println("Errors detected:");
            for (String error : errors) {
                System.out.println("  - " + error);
            }
            System.out.println();
            System.out.println("TEST FAILED: Encoding issues detected");
            System.exit(1);
        } else {
            System.out.println();
            System.out.println("TEST PASSED: All products decoded correctly");
            System.exit(0);
        }
    }
    
    /**
     * Validates that a product description is correctly decoded.
     * 
     * @param productId The product ID
     * @param description The decoded description
     * @return List of validation errors (empty if valid)
     */
    private static List<String> validateDescription(String productId, String description) {
        List<String> errors = new ArrayList<>();
        
        // Check for replacement characters (indicates decoding failure)
        if (description.contains(String.valueOf(REPLACEMENT_CHAR))) {
            errors.add("Contains replacement character (U+FFFD) - encoding error");
        }
        
        // Check for expected special characters
        // All products should have trademark and euro symbols
        boolean hasTrademark = description.contains(TRADEMARK);
        boolean hasEuro = description.contains(EURO);
        boolean hasEmDash = description.contains(EM_DASH);
        
        // Check for garbled sequences that indicate Windows-1252 decoded as UTF-8
        // When Windows-1252 ™ (0x99) is decoded as UTF-8, it becomes invalid
        // When Windows-1252 € (0x80) is decoded as UTF-8, it becomes invalid
        
        // Look for common garbled patterns
        // When Windows-1252 special chars are decoded as UTF-8, they produce specific garbled sequences
        // Using hex escapes to avoid encoding issues in source file
        String garbledTrademark = "\u00E2\u0084\u00A2";  // â„¢
        String garbledEuro = "\u00E2\u0082\u00AC";       // â‚¬
        String garbledEmDash = "\u00E2\u0080\u0094";     // â€"
        
        if (description.contains(garbledTrademark)) {
            errors.add("Contains garbled trademark - Windows-1252 decoded as UTF-8");
        }
        if (description.contains(garbledEuro)) {
            errors.add("Contains garbled euro - Windows-1252 decoded as UTF-8");
        }
        if (description.contains(garbledEmDash)) {
            errors.add("Contains garbled em-dash - Windows-1252 decoded as UTF-8");
        }
        
        // If no special characters found and no garbled patterns,
        // the encoding was likely wrong (chars may have been dropped or replaced)
        if (!hasTrademark && !hasEuro && !hasEmDash) {
            // Check if description has any high-byte characters that might indicate issues
            boolean hasHighBytes = false;
            for (char c : description.toCharArray()) {
                if (c > 127 && c != REPLACEMENT_CHAR) {
                    hasHighBytes = true;
                    break;
                }
            }
            if (!hasHighBytes) {
                errors.add("Missing expected special characters (™, €, —) - possible encoding error");
            }
        }
        
        return errors;
    }
}
