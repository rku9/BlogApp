package com.blogapp.services;

import com.blogapp.models.Role;
import com.blogapp.models.User;
import com.blogapp.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;

  public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
    this.userRepository = userRepository;
    this.bCryptPasswordEncoder = bCryptPasswordEncoder;
  }

  public User register(
      String name, String email, String password, String confirmPassword, Role userRole) {
    String normalizedEmail = email == null ? null : email.trim().toLowerCase();
    Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);
    if (optionalUser.isPresent()) {
      return optionalUser.get();
    }
    if (!password.equals(confirmPassword)) {
      throw new IllegalArgumentException("Passwords do not match. Please enter the same password.");
    }
    if (userRole == null) {
      userRole = Role.AUTHOR;
    }
    User user = new User();
    user.setName(name);
    user.setEmail(normalizedEmail);
    user.setPassword(bCryptPasswordEncoder.encode(password));
    user.setUserRole(userRole);
    return userRepository.save(user);
  }

  public User login(String email, String passedPassword) {
    String normalizedEmail = email == null ? null : email.trim().toLowerCase();
    User user =
        userRepository
            .findByEmail(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

    if (bCryptPasswordEncoder.matches(passedPassword, user.getPassword())) {
      return user;
    }

    if (user.getPassword().equals(passedPassword)) {
      user.setPassword(bCryptPasswordEncoder.encode(passedPassword));
      return userRepository.save(user);
    }

    throw new IllegalArgumentException("Invalid email or password.");
  }

  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  public List<User> findAllUsers() {
    return userRepository.findAll();
  }
}
