package src;

import lib.EventServer;
import java.time.LocalDateTime;

/**
 * Test program that verifies event timestamp conversion.
 */
public class ClientTest {
    
    public static void main(String[] args) {
        EventServer server = new EventServer();
        EventClient client = new EventClient(server, "Asia/Shanghai");
        
        EventServer.Event event = client.getEvent();
        
        System.out.println("Event ID: " + event.getId());
        System.out.println("Raw timestamp: " + event.getTimestamp());
        
        LocalDateTime clientTime = client.convertToDisplayTime(event.getTimestamp());
        
        System.out.println("Display time: " + clientTime);
        
        // Expected values from reference implementation (do not modify)
        final int EXPECTED_HOUR = 2;
        final int EXPECTED_DAY = 16;
        
        int actualHour = clientTime.getHour();
        int actualDay = clientTime.getDayOfMonth();
        
        if (actualDay != EXPECTED_DAY || actualHour != EXPECTED_HOUR) {
            System.out.println();
            System.out.println("ERROR: Time mismatch!");
            System.out.println("Expected: " + EXPECTED_DAY + " day, " + String.format("%02d", EXPECTED_HOUR) + ":00");
            System.out.println("Got: " + actualDay + " day, " + String.format("%02d", actualHour) + ":00");
            System.exit(1);
        }
        
        System.out.println("SUCCESS: Time conversion correct");
        System.exit(0);
    }
}
