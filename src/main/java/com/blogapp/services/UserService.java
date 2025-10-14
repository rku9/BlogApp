package com.blogapp.services;

import com.blogapp.exceptions.UserNotFoundException;
import com.blogapp.models.User;
import com.blogapp.repositories.UserRepository;

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

  public User login(String email, String password) {
    if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
      throw new IllegalArgumentException("Email and password are required.");
    }


    Optional<User> optionalUser = userRepository.findByEmail(email.trim().toLowerCase());
    if (optionalUser.isEmpty()) {
      throw new UserNotFoundException("User not found for email.");
    }

    User user = optionalUser.get();
    boolean passwordMatch = bCryptPasswordEncoder.matches(password, user.getPassword());
    if (!passwordMatch) {
      throw new IllegalArgumentException("Invalid password.");
    }

    return user;
  }

  public User register(String name, String email, String password, String confirmPassword) {
    Optional<User> optionalUser = userRepository.findByEmail(email);
    if (optionalUser.isPresent()) {
      return optionalUser.get();
    }
    if (!password.equals(confirmPassword)) {
      throw new IllegalArgumentException("Passwords do not match. Please enter the same password.");
    }
    User user = new User();
    user.setName(name);
    user.setEmail(email);
    user.setPassword(bCryptPasswordEncoder.encode(password));
    return userRepository.save(user);
  }

  //    public User login(String email, String password){
  //        Optional<User> optionalUser = userRepository.findByEmail(email);
  //        if(optionalUser.isEmpty()){
  //            return null;
  //        }
  //        User user = optionalUser.get();
  //        boolean passwordMatch = bCryptPasswordEncoder.matches(password, user.getPassword());
  //        if(passwordMatch){
  //            return user;
  //        }
  //        return null;
  //    }
}
