import java.io.*;
import java.net.*;

/**
 * Client for receiving binary compressed data from the streaming server.
 * 
 * Protocol documentation:
 * - Send 4-byte integer: number of bytes requested
 * - Receive 4-byte integer: total length of data that follows
 * - Receive raw data bytes (length specified in header)
 * 
 * The server sends data with a simple length-prefixed format for efficient
 * streaming of compressed binary content.
 */
public class StreamDecoder {
    
    private final String host;
    private final int port;
    
    public StreamDecoder(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * Requests binary data from the server.
     * 
     * @param requestedBytes the number of bytes to request
     * @return the raw data received from the server
     * @throws IOException if communication fails
     */
    public byte[] fetchData(int requestedBytes) throws IOException {
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            
            out.writeInt(requestedBytes);
            out.flush();
            
            int dataLength = in.readInt();
            
            byte[] data = new byte[dataLength];
            int totalRead = 0;
            
            while (totalRead < dataLength) {
                int available = in.available();
                if (available == 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    available = in.available();
                    if (available == 0) {
                        int singleByte = in.read();
                        if (singleByte == -1) {
                            break;
                        }
                        data[totalRead++] = (byte) singleByte;
                        continue;
                    }
                }
                
                int toRead = Math.min(available, dataLength - totalRead);
                int bytesRead = in.read(data, totalRead, toRead);
                if (bytesRead == -1) {
                    break;
                }
                totalRead += bytesRead;
            }
            
            if (totalRead < dataLength) {
                byte[] truncated = new byte[totalRead];
                System.arraycopy(data, 0, truncated, 0, totalRead);
                return truncated;
            }
            
            return data;
        }
    }
    
    /**
     * Validates that data matches the expected sequential pattern.
     * Expected pattern: byte[i] = (i % 256)
     */
    public static boolean validatePattern(byte[] data, int expectedLength) {
        if (data.length != expectedLength) {
            System.err.println("Length mismatch: expected " + expectedLength + 
                             ", got " + data.length);
            return false;
        }
        
        for (int i = 0; i < data.length; i++) {
            byte expected = (byte)(i % 256);
            if (data[i] != expected) {
                System.err.println("Pattern mismatch at index " + i + 
                                 ": expected " + (expected & 0xFF) + 
                                 ", got " + (data[i] & 0xFF));
                return false;
            }
        }
        
        return true;
    }
}
