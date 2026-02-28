package Iservices.User;



import Entities.User.User;

import java.util.List;



public interface IUserService {



    boolean addUser(User user);



    List<User> getAllUsers();



    boolean updateUser(User user);



    boolean deleteUser(int id);



    User login(String email, String password);

    User loginWithIdentifier(String identifier, String password);

    User getUserById(int id);



    boolean emailExists(String email);


    boolean phoneExists(String phone);



    boolean fullNameExists(String fullName);



    boolean emailExistsForOther(String email, int id);



    boolean phoneExistsForOther(String phone, int id);



    boolean fullNameExistsForOther(String fullName, int id);

}

