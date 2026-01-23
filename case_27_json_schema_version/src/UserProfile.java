/**
 * Data class representing a user profile from the API.
 * Contains all fields that a complete user profile should have.
 */
public class UserProfile {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String department;
    private String role;
    
    public UserProfile() {}
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    @Override
    public String toString() {
        return "UserProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", department='" + department + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
