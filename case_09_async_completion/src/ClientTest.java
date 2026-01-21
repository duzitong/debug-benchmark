package src;

import lib.JobServer;

/**
 * ClientTest - Demonstrates job submission to the server
 * 
 * This test submits 5 jobs and expects all of them to complete successfully.
 * The jobs are a mix of fast and slow operations.
 * 
 * Expected behavior: All jobs should return results immediately.
 */
public class ClientTest {
    
    private static final int SERVER_PORT = 8089;
    
    public static void main(String[] args) {
        System.out.println("=== Job Processing Client Test ===\n");
        
        JobServer server = null;
        int exitCode = 0;
        
        try {
            // Start server
            System.out.println("Starting server on port " + SERVER_PORT + "...");
            server = new JobServer(SERVER_PORT);
            server.start();
            Thread.sleep(500);  // Give server time to start
            
            JobClient client = new JobClient("http://localhost:" + SERVER_PORT);
            
            // Define test jobs - mix of fast and slow
            // Duration is just a hint for job complexity, all jobs return immediately
            String[] jobNames = {"job-1", "job-2", "job-3", "job-4", "job-5"};
            int[] durations = {500, 300, 2000, 400, 2500}; // milliseconds
            
            int successCount = 0;
            int failCount = 0;
            
            for (int i = 0; i < jobNames.length; i++) {
                String jobName = jobNames[i];
                int duration = durations[i];
                
                System.out.println("Submitting " + jobName + " (duration hint: " + duration + "ms)...");
                
                try {
                    JobClient.JobResponse response = client.submitJob(jobName, duration);
                    System.out.println("  SUCCESS: " + response.getResult());
                    successCount++;
                } catch (JobClient.JobException e) {
                    System.out.println("  FAILED: " + e.getMessage());
                    failCount++;
                } catch (Exception e) {
                    System.out.println("  ERROR: " + e.getMessage());
                    failCount++;
                }
            }
            
            System.out.println("\n=== Test Summary ===");
            System.out.println("Successful: " + successCount);
            System.out.println("Failed: " + failCount);
            System.out.println("Total: " + jobNames.length);
            
            if (failCount > 0) {
                System.out.println("\nERROR: Some jobs failed to complete!");
                System.out.println("This is unexpected - all jobs should return results.");
                exitCode = 1;
            } else {
                System.out.println("\nAll jobs completed successfully!");
                exitCode = 0;
            }
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            exitCode = 1;
        } finally {
            if (server != null) {
                server.stop();
            }
        }
        
        System.exit(exitCode);
    }
}
