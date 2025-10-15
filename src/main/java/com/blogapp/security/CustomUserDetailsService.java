package com.blogapp.security;

import com.blogapp.models.User;
import com.blogapp.repositories.UserRepository;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<User> optionalUser = userRepository.findByEmail(username.toLowerCase());
    User user =
        optionalUser.orElseThrow(
            () -> new UsernameNotFoundException("User not found with email: " + username));
    return new CustomUserDetails(user);
  }
}
