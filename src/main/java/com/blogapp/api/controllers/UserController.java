package com.blogapp.api.controllers;

import com.blogapp.dtos.LoginRequestDto;
import com.blogapp.dtos.LoginResponseDto;
import com.blogapp.models.User;
import com.blogapp.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("apiUserController")
@RequestMapping("/api/auth")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
    try {
      User user = userService.login(loginRequestDto.getEmail(), loginRequestDto.getPassword());
      LoginResponseDto responseDto =
          new LoginResponseDto(
              user.getId(),
              user.getName(),
              user.getEmail(),
              user.getUserRole(),
              true,
              "Login successful.");
      return ResponseEntity.ok(responseDto);
    } catch (IllegalStateException e) {
      LoginResponseDto responseDto =
          new LoginResponseDto(null, null, null, null, false, e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseDto);
    } catch (IllegalArgumentException e) {
      LoginResponseDto responseDto =
          new LoginResponseDto(null, null, null, null, false, e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDto);
    }
  }
}
