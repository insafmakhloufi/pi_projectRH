package Entities.User;
public class Admin extends User {
    public Admin(int id, String fullName, String email, String phone, String profilePhoto, String password) {
        super(id, fullName, email, phone, profilePhoto, password, "ADMIN");
    }

    @Override
    public boolean canDeleteUser() { return true; }

    @Override
    public boolean canManageUsers() { return true; }
}
