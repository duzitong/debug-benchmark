import java.util.*;

/**
 * Interface for CORS server operations.
 * 
 * This interface defines the contract between the client and server
 * for handling cross-origin requests. The client only has visibility
 * into these methods - the server implementation is opaque.
 * 
 * The client cannot manipulate server state directly; it can only
 * send requests and receive responses.
 */
public interface CorsServerInterface {
    
    /**
     * Handle OPTIONS preflight request.
     * Returns the CORS preflight response headers indicating allowed methods.
     * 
     * @param requestedMethod The HTTP method the client intends to use
     * @param origin The origin of the requesting site
     * @return CorsPreflightResponse containing allowed methods and cache hints
     */
    CorsPreflightResponse handlePreflight(String requestedMethod, String origin);
    
    /**
     * Handle actual request (GET, POST, PUT, etc.)
     * Validates method against current CORS policy.
     * 
     * @param method The HTTP method being used
     * @param origin The origin of the requesting site
     * @param body The request body
     * @return CorsRequestResponse containing result or CORS error
     */
    CorsRequestResponse handleRequest(String method, String origin, String body);
}

/**
 * Response from a CORS preflight (OPTIONS) request
 */
class CorsPreflightResponse {
    private String allowOrigin;
    private List<String> allowMethods;
    private int maxAge;
    private boolean success;
    
    public String getAllowOrigin() { return allowOrigin; }
    public void setAllowOrigin(String allowOrigin) { this.allowOrigin = allowOrigin; }
    
    public List<String> getAllowMethods() { return allowMethods; }
    public void setAllowMethods(List<String> allowMethods) { this.allowMethods = allowMethods; }
    
    public int getMaxAge() { return maxAge; }
    public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}

/**
 * Response from an actual CORS request
 */
class CorsRequestResponse {
    private int statusCode;
    private String body;
    private String allowOrigin;
    private boolean corsError;
    private String errorMessage;
    
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public String getAllowOrigin() { return allowOrigin; }
    public void setAllowOrigin(String allowOrigin) { this.allowOrigin = allowOrigin; }
    
    public boolean isCorsError() { return corsError; }
    public void setCorsError(boolean corsError) { this.corsError = corsError; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
