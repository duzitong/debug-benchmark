import server.BatchResult;
import server.OperationResult;

/**
 * Test program for ConfigDeployClient.
 * Demonstrates batch configuration deployment.
 * 
 * The client assumes operations can be batched in any order,
 * but the server has implicit ordering requirements.
 */
public class ClientTest {
    
    public static void main(String[] args) {
        System.out.println("=== Configuration Deployment Test ===");
        System.out.println();
        
        ConfigDeployClient client = new ConfigDeployClient();
        
        // The client batches operations together, assuming
        // the server will process them correctly regardless of order.
        // 
        // Bug: The client sends operations in an order that violates
        // hidden server-side constraints (dependencies and state requirements).
        //
        // Config IDs are opaque hashes - the client doesn't know which
        // configs depend on which others.
        
        System.out.println("Queueing operations for config deployment...");
        
        // Queue operations in the order the client thinks is correct:
        // First validate all configs, then deploy all configs
        
        // Validate all configs first
        client.validate("cfg-5e1a");  // Has hidden dependencies on cfg-3b8e and cfg-9c4d
        client.validate("cfg-9c4d");  // Has hidden dependency on cfg-7a2f
        client.validate("cfg-7a2f");  // No dependencies (but processed too late)
        client.validate("cfg-3b8e");  // No dependencies (but processed too late)
        
        // Then deploy all configs
        client.deploy("cfg-7a2f");
        client.deploy("cfg-3b8e");
        client.deploy("cfg-9c4d");
        client.deploy("cfg-5e1a");
        
        System.out.println("Executing batch...");
        System.out.println();
        
        BatchResult result = client.executeBatch();
        client.printResults(result);
        
        // Check if all operations succeeded
        int failedCount = 0;
        for (OperationResult opResult : result.getResults()) {
            if (!opResult.isSuccess()) {
                failedCount++;
            }
        }
        
        System.out.println();
        if (failedCount > 0) {
            System.out.println("ERROR: " + failedCount + " operations failed.");
            System.out.println("BATCH_ERROR: Some operations could not be completed.");
            System.exit(1);
        } else {
            System.out.println("All operations completed successfully.");
            System.exit(0);
        }
    }
}
