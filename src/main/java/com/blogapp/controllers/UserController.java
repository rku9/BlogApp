package com.blogapp.controllers;

import com.blogapp.dtos.LoginRequestDto;
import com.blogapp.dtos.SignUpRequestDto;
import com.blogapp.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/login")
  public String login(Model model) {
    model.addAttribute("loginRequestDto", new LoginRequestDto());
    return "login";
  }

  @PostMapping("/login")
  public String login(LoginRequestDto loginRequestDto, Model model) {
    String email = loginRequestDto.getEmail();
    String password = loginRequestDto.getPassword();

    try {
      userService.login(email, password);
      return "redirect:/";
    } catch (IllegalArgumentException e) {
      model.addAttribute("errorMessage", e.getMessage());
      model.addAttribute("loginRequestDto", loginRequestDto);
      return "login";
    }
  }

  @GetMapping("/register")
  public String register(Model model) {
    model.addAttribute("signUpRequestDto", new SignUpRequestDto());
    return "register";
  }

  @PostMapping("/register")
  public String registerUser(SignUpRequestDto signUpRequestDto, Model model) {
    String name = signUpRequestDto.getName();
    String email = signUpRequestDto.getEmail();
    String password = signUpRequestDto.getPassword();
    String confirmPassword = signUpRequestDto.getConfirmPassword();

    try {
      userService.register(name, email, password, confirmPassword);
      return "redirect:/";
    } catch (IllegalArgumentException e) {
      model.addAttribute("errorMessage", e.getMessage());
      model.addAttribute("signUpRequestDto", signUpRequestDto);
      return "register";
    }
  }
}
