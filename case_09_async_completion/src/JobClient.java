package src;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * JobClient - Client for the Job Processing API
 * 
 * This client submits jobs to the server and retrieves results.
 * The API is synchronous - all job submissions return results immediately.
 * 
 * Usage:
 *   JobClient client = new JobClient("http://localhost:8080");
 *   JobResponse response = client.submitJob("myJob", 500);
 *   System.out.println(response.getResult());
 */
public class JobClient {
    private final String baseUrl;
    private final int timeoutMs;
    
    /**
     * Create a new JobClient.
     * @param baseUrl The base URL of the job server (e.g., "http://localhost:8080")
     */
    public JobClient(String baseUrl) {
        this(baseUrl, 5000);
    }
    
    /**
     * Create a new JobClient with custom timeout.
     * @param baseUrl The base URL of the job server
     * @param timeoutMs Request timeout in milliseconds
     */
    public JobClient(String baseUrl, int timeoutMs) {
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
    }
    
    /**
     * Submit a job for processing and get the result.
     * 
     * The server processes jobs synchronously and returns results in the response.
     * All successful responses contain the result data directly.
     * 
     * @param jobName Name of the job
     * @param duration Expected duration hint (not used by client, just metadata)
     * @return JobResponse containing the result
     * @throws IOException if the request fails
     * @throws JobException if the job processing fails
     */
    public JobResponse submitJob(String jobName, int duration) throws IOException, JobException {
        String endpoint = baseUrl + "/api/jobs";
        String requestBody = "{\"name\": \"" + jobName + "\", \"duration\": " + duration + "}";
        
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setDoOutput(true);
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes("UTF-8"));
        }
        
        int statusCode = conn.getResponseCode();
        
        // Read response
        String responseBody;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            responseBody = sb.toString();
        }
        
        if (statusCode != 200) {
            throw new JobException("Server returned error: " + statusCode);
        }
        
        // Parse response - expect resultReady: true with result data
        return parseResponse(responseBody, jobName);
    }
    
    /**
     * Parse the server response.
     * 
     * Expected format:
     * {
     *   "resultReady": true,
     *   "result": {
     *     "data": "..."
     *   }
     * }
     * 
     * If resultReady is not true, it means the job failed to process.
     */
    private JobResponse parseResponse(String responseBody, String jobName) throws JobException {
        // Check if resultReady is true
        boolean resultReady = responseBody.contains("\"resultReady\": true") || 
                              responseBody.contains("\"resultReady\":true");
        
        if (!resultReady) {
            // Job did not complete - this shouldn't happen with our synchronous API
            // but we handle it as an error case
            throw new JobException("Job '" + jobName + "' did not return a result. " +
                                   "The server may be overloaded or the job failed.");
        }
        
        // Extract result data
        String data = extractResultData(responseBody);
        return new JobResponse(jobName, data);
    }
    
    /**
     * Extract the data field from the result object.
     */
    private String extractResultData(String responseBody) {
        try {
            // Look for "data": "..."
            int dataIdx = responseBody.indexOf("\"data\"");
            if (dataIdx >= 0) {
                int quoteStart = responseBody.indexOf("\"", responseBody.indexOf(":", dataIdx) + 1);
                int quoteEnd = responseBody.indexOf("\"", quoteStart + 1);
                if (quoteStart >= 0 && quoteEnd > quoteStart) {
                    return responseBody.substring(quoteStart + 1, quoteEnd);
                }
            }
        } catch (Exception e) {
            // ignore parsing errors
        }
        return "unknown";
    }
    
    /**
     * Response from a job submission.
     */
    public static class JobResponse {
        private final String jobName;
        private final String result;
        
        public JobResponse(String jobName, String result) {
            this.jobName = jobName;
            this.result = result;
        }
        
        public String getJobName() {
            return jobName;
        }
        
        public String getResult() {
            return result;
        }
        
        @Override
        public String toString() {
            return "JobResponse{jobName='" + jobName + "', result='" + result + "'}";
        }
    }
    
    /**
     * Exception thrown when a job fails.
     */
    public static class JobException extends Exception {
        public JobException(String message) {
            super(message);
        }
    }
}
