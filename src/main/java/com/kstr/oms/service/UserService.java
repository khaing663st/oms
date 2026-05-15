package com.kstr.oms.service;

import com.kstr.oms.dto.UserDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {

    String saveUser(UserDTO userDTO);

    Optional<UserDTO> getUserById(String userId);

    Optional<UserDTO> updateUser(String userId, UserDTO userDTO);

    boolean deleteUser(String userId);

    List<UserDTO> getAllUsers();

}

