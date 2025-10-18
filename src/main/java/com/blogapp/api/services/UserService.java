//package com.blogapp.api.services;
//
//import com.blogapp.models.User;
//import com.blogapp.repositories.UserRepository;
//
//import java.util.Optional;
//
//public class UserService {
//
//    private final UserRepository userRepository;
//
////    public UserService(UserRepository userRepository){
////        this.userRepository = userRepository;
////    }
//
//    public User login(String email, String password){
//        Optional<User> optionalUser = userRepository.findByEmail(email);
//        User user = optionalUser.orElseThrow(() -> new IllegalArgumentException("User not found"));
//
//    }
//}
