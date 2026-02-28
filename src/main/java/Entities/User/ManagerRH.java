package Entities;

public class ManagerRH extends User {
    public ManagerRH(int id, String fullName, String email, String phone, String profilePhoto, String password) {
        super(id, fullName, email, phone, profilePhoto, password, "MANAGER_RH");
    }

    @Override
    public boolean canManageUsers() { return true; }
}
