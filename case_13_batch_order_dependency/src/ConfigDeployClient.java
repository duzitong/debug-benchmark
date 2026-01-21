import server.BatchOperation;
import server.BatchResult;
import server.ConfigDeployServer;
import server.OperationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for deploying configuration items.
 * Sends batch operations to the server for processing.
 */
public class ConfigDeployClient {
    private final ConfigDeployServer server;
    private final List<BatchOperation> pendingOperations;
    private int operationCounter;

    public ConfigDeployClient() {
        this.server = new ConfigDeployServer();
        this.pendingOperations = new ArrayList<>();
        this.operationCounter = 0;
    }

    /**
     * Add a VALIDATE operation for a config item.
     */
    public String validate(String configId) {
        String opId = "op-" + (++operationCounter);
        pendingOperations.add(new BatchOperation(opId, configId, "VALIDATE"));
        return opId;
    }

    /**
     * Add a DEPLOY operation for a config item.
     */
    public String deploy(String configId) {
        String opId = "op-" + (++operationCounter);
        pendingOperations.add(new BatchOperation(opId, configId, "DEPLOY"));
        return opId;
    }

    /**
     * Execute all pending operations as a batch.
     * Returns the batch result with individual operation outcomes.
     */
    public BatchResult executeBatch() {
        List<BatchOperation> batch = new ArrayList<>(pendingOperations);
        pendingOperations.clear();
        return server.executeBatch(batch);
    }

    /**
     * Reset server state for testing.
     */
    public void resetServer() {
        server.reset();
        pendingOperations.clear();
        operationCounter = 0;
    }

    /**
     * Print batch results in a generic format.
     */
    public void printResults(BatchResult result) {
        System.out.println("Batch completed. Success: " + result.isAllSucceeded());
        for (OperationResult opResult : result.getResults()) {
            if (opResult.isSuccess()) {
                System.out.println("  " + opResult.getOperationId() + ": OK");
            } else {
                System.out.println("  " + opResult.getOperationId() + ": " + opResult.getErrorCode());
            }
        }
    }
}
