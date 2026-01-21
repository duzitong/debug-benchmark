package src;

import lib.EventServer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Client that retrieves events from the server and converts timestamps
 * for display in the user's local timezone.
 */
public class EventClient {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    private final EventServer server;
    private final ZoneId displayTimezone;
    private final ZoneId serverTimezone;
    
    /**
     * Creates a new EventClient.
     * @param server the event server to connect to
     * @param displayTimezone the timezone to use for displaying times to the user
     */
    public EventClient(EventServer server, String displayTimezone) {
        this.server = server;
        this.displayTimezone = ZoneId.of(displayTimezone);
        this.serverTimezone = ZoneId.of(server.getServerTimezone());
    }
    
    /**
     * Retrieves an event from the server.
     * @return the event
     */
    public EventServer.Event getEvent() {
        return server.getEvent();
    }
    
    /**
     * Converts a timestamp string from server timezone to the display timezone.
     * @param timestamp the timestamp string from the server
     * @return LocalDateTime in the display timezone
     */
    public LocalDateTime convertToDisplayTime(String timestamp) {
        LocalDateTime serverTime = LocalDateTime.parse(timestamp, FORMATTER);
        ZonedDateTime serverZoned = serverTime.atZone(serverTimezone);
        ZonedDateTime displayZoned = serverZoned.withZoneSameInstant(displayTimezone);
        return displayZoned.toLocalDateTime();
    }
    
    /**
     * Gets the configured server timezone.
     * @return the server timezone ID
     */
    public ZoneId getServerTimezone() {
        return serverTimezone;
    }
    
    /**
     * Gets the display timezone.
     * @return the display timezone ID
     */
    public ZoneId getDisplayTimezone() {
        return displayTimezone;
    }
}
