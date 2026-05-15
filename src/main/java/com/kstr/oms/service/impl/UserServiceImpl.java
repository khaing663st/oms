package com.kstr.oms.service.impl;

import com.kstr.oms.domain.User;
import com.kstr.oms.dto.UserDTO;
import com.kstr.oms.mapper.UserMapper;
import com.kstr.oms.repository.UserRepository;
import com.kstr.oms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    @Override
    public String saveUser(UserDTO userDTO) {
        try {
            User user = userMapper.toEntity(userDTO);
            User savedUser = userRepository.save(user);
            log.info("User created with userId ::: {}", savedUser.getUserId());
            return savedUser.getUserId();
        } catch (Exception e) {
            log.error("Error creating user ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<UserDTO> getUserById(String userId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            log.info("User retrieved with userId ::: {}", userId);
            return user.map(userMapper::toDTO);
        } catch (Exception e) {
            log.error("Error retrieving user ::: ", e);
            throw e;
        }
    }

    @Override
    public Optional<UserDTO> updateUser(String userId, UserDTO userDTO) {
        try {
            User user = userMapper.toEntity(userDTO);
            user.setUserId(userId);
            Optional<User> updatedUser = userRepository.update(userId, user);
            log.info("User updated with userId ::: {}", userId);
            return updatedUser.map(userMapper::toDTO);
        } catch (Exception e) {
            log.error("Error updating user ::: ", e);
            throw e;
        }
    }

    @Override
    public boolean deleteUser(String userId) {
        try {
            boolean deleted = userRepository.delete(userId);
            log.info("User deleted with userId ::: {}", userId);
            return deleted;
        } catch (Exception e) {
            log.error("Error deleting user ::: ", e);
            throw e;
        }
    }

    @Override
    public List<UserDTO> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            log.info("Retrieved {} user counts", users.size());
            return users.stream()
                    .map(userMapper::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error retrieving all users ::: ", e);
            throw e;
        }
    }
}

