package Entities.User;

public class User {

    private int id;
    private String fullName;
    private String titre;
    private String email;
    private String phone;
    private String profilePhoto;
    private String password;
    private String role;

    public User(int id, String fullName, String email, String phone, String profilePhoto, String password, String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.profilePhoto = profilePhoto;
        this.password = password;
        this.role = role;
    }

    public User(int id, String fullName, String titre, String email, String phone, String profilePhoto, String password, String role) {
        this.id = id;
        this.fullName = fullName;
        this.titre = titre;
        this.email = email;
        this.phone = phone;
        this.profilePhoto = profilePhoto;
        this.password = password;
        this.role = role;
    }

    public User(String fullName, String email, String phone, String profilePhoto, String password, String role) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.profilePhoto = profilePhoto;
        this.password = password;
        this.role = role;
    }

    public User(String fullName, String titre, String email, String phone, String profilePhoto, String password, String role) {
        this.fullName = fullName;
        this.titre = titre;
        this.email = email;
        this.phone = phone;
        this.profilePhoto = profilePhoto;
        this.password = password;
        this.role = role;
    }

    // Getters
    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getTitre() { return titre; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getProfilePhoto() { return profilePhoto; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    // Setters
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }

    // Role-based default permissions (can be overridden in subclasses)
    public boolean canDeleteUser() { return false; }
    public boolean canManageUsers() { return false; }
}
