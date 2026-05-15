package com.kstr.oms.repository;

import com.kstr.oms.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(String userId);

    Optional<User> update(String userId, User user);

    boolean delete(String userId);

    List<User> findAll();

}

