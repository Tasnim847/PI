package org.example.projet_pi.Service;

import org.example.projet_pi.entity.User;

import java.util.List;

public interface IUserService {

    User addUser(User user);

    User updateUser(User user);

    void deleteUser(Long id);

    User getUserById(Long id);

    List<User> getAllUsers();

    User updateUserById(Long id, User user);
}
