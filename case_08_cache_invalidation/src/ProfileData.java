import java.io.Serializable;

public class ProfileData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String name;
    private String email;
    private int version;
    private long lastModified;
    
    public ProfileData() {}
    
    public ProfileData(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.version = 1;
        this.lastModified = System.currentTimeMillis();
    }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    
    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    
    @Override
    public String toString() {
        return "ProfileData{userId='" + userId + "', name='" + name + 
               "', email='" + email + "', version=" + version + 
               ", lastModified=" + lastModified + "}";
    }
}
