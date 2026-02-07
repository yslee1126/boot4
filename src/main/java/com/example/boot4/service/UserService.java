package com.example.boot4.service;

import com.example.boot4.domain.User;
import com.example.boot4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User createUser(String name) {
        return userRepository.save(new User(name));
    }

    public List<User> getAllUsers() {
        // Using QueryDSL implementation
        return userRepository.findAllQueryDsl();
    }

    public User getUserByName(String name) {
        // Using QueryDSL implementation
        return userRepository.findByName(name);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public User updateUser(Long id, String name) {
        User user = getUserById(id);
        user.updateName(name);
        return user;
    }
}
