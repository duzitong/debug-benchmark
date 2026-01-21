import java.util.Map;
import java.util.HashMap;

/**
 * Represents an HTTP response from the API.
 */
public class ApiResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;

    public ApiResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = new HashMap<>();
    }

    public ApiResponse(int statusCode, String body, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers != null ? headers : new HashMap<>();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean isRateLimited() {
        return statusCode == 429;
    }
}
