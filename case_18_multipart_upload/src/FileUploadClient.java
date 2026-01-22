import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class FileUploadClient {
    
    private static final int CHUNK_SIZE = 1024 * 1024;
    
    private final String serverBaseUrl;
    private final ExecutorService executor;
    
    public FileUploadClient(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
        this.executor = Executors.newFixedThreadPool(4);
    }
    
    public void uploadFile(String fileId, byte[] fileData) throws Exception {
        int totalChunks = (int) Math.ceil((double) fileData.length / CHUNK_SIZE);
        List<Future<?>> futures = new ArrayList<>();
        
        for (int i = 0; i < totalChunks; i++) {
            final int partNum = i;
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, fileData.length);
            byte[] chunkData = Arrays.copyOfRange(fileData, start, end);
            
            futures.add(executor.submit(() -> {
                try {
                    uploadChunk(fileId, partNum, chunkData);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to upload chunk " + partNum, e);
                }
            }));
        }
        
        for (Future<?> future : futures) {
            future.get();
        }
        
        completeUpload(fileId);
    }
    
    private void uploadChunk(String fileId, int partNum, byte[] chunkData) throws Exception {
        URL url = new URL(serverBaseUrl + "/upload/" + fileId + "/chunk/" + partNum);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(chunkData);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Chunk upload failed with status: " + responseCode);
        }
        
        conn.disconnect();
    }
    
    private void completeUpload(String fileId) throws Exception {
        URL url = new URL(serverBaseUrl + "/upload/" + fileId + "/complete");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(new byte[0]);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Complete request failed with status: " + responseCode);
        }
        
        conn.disconnect();
    }
    
    public byte[] downloadFile(String fileId) throws Exception {
        URL url = new URL(serverBaseUrl + "/upload/" + fileId + "/download");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Download failed with status: " + responseCode);
        }
        
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] temp = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(temp)) != -1) {
                buffer.write(temp, 0, bytesRead);
            }
            return buffer.toByteArray();
        } finally {
            conn.disconnect();
        }
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}
