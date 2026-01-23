import java.util.*;

/**
 * Transaction Client for Database Operations
 * 
 * This client provides a simple API for performing transactional database queries.
 * All queries within a transaction are expected to see consistent data (REPEATABLE READ).
 * 
 * Usage:
 *   TransactionClient client = new TransactionClient();
 *   client.beginTransaction();
 *   int count = client.getItemCount();
 *   int sum = client.getItemSum();
 *   double average = client.calculateAverage(count, sum);
 *   client.commit();
 */
public class TransactionClient {
    
    private String currentTransactionId;
    private boolean inTransaction;
    
    public TransactionClient() {
        this.currentTransactionId = null;
        this.inTransaction = false;
    }
    
    /**
     * Begins a new database transaction.
     * All subsequent queries will be part of this transaction.
     * The transaction guarantees consistent reads across all queries.
     */
    public void beginTransaction() {
        if (inTransaction) {
            throw new IllegalStateException("Transaction already in progress");
        }
        
        // Begin transaction with default settings
        // Transaction should provide REPEATABLE READ semantics for consistent queries
        currentTransactionId = DatabaseServer.beginTransaction(null);
        inTransaction = true;
    }
    
    /**
     * Gets the count of all items in the database.
     * This query is part of the current transaction and sees a consistent snapshot.
     * 
     * @return Number of items in the database
     */
    public int getItemCount() {
        ensureInTransaction();
        
        // Execute query within transaction context
        // All queries in the same transaction should see the same data snapshot
        return DatabaseServer.getItemCount(currentTransactionId, null);
    }
    
    /**
     * Gets the sum of all item values in the database.
     * This query is part of the current transaction and sees the same snapshot as other queries.
     * 
     * @return Sum of all item values
     */
    public int getItemSum() {
        ensureInTransaction();
        
        // Execute query within transaction context
        // This should return sum consistent with the count from the same transaction
        return DatabaseServer.getItemSum(currentTransactionId, null);
    }
    
    /**
     * Calculates the average value per item.
     * Since both count and sum come from the same transaction snapshot,
     * the average should be accurate.
     * 
     * @param count Number of items
     * @param sum Total sum of item values
     * @return Average value per item
     */
    public double calculateAverage(int count, int sum) {
        if (count == 0) {
            return 0.0;
        }
        return (double) sum / count;
    }
    
    /**
     * Commits the current transaction.
     * All changes made during this transaction are persisted.
     */
    public void commit() {
        ensureInTransaction();
        DatabaseServer.commit(currentTransactionId);
        currentTransactionId = null;
        inTransaction = false;
    }
    
    /**
     * Rolls back the current transaction.
     * All changes made during this transaction are discarded.
     */
    public void rollback() {
        ensureInTransaction();
        DatabaseServer.rollback(currentTransactionId);
        currentTransactionId = null;
        inTransaction = false;
    }
    
    /**
     * Checks if a transaction is currently active.
     * 
     * @return true if in a transaction, false otherwise
     */
    public boolean isInTransaction() {
        return inTransaction;
    }
    
    private void ensureInTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("No transaction in progress");
        }
    }
}
