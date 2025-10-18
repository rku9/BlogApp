package com.blogapp.controllers;

import com.blogapp.dtos.LoginRequestDto;
import com.blogapp.dtos.SignUpRequestDto;
import com.blogapp.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/login")
  public String login(Model model, @RequestParam(value = "error", required = false) String error) {
    model.addAttribute("loginRequestDto", new LoginRequestDto());
    if (error != null) {
      model.addAttribute("errorMessage", "Invalid email or password");
    }
    return "login";
  }

  @GetMapping("/register")
  public String register(Model model) {
    model.addAttribute("signUpRequestDto", new SignUpRequestDto());
    return "register";
  }

  @PostMapping("/register")
  public String registerUser(@ModelAttribute SignUpRequestDto signUpRequestDto, Model model) {
    String name = signUpRequestDto.getName();
    String email = signUpRequestDto.getEmail();
    String password = signUpRequestDto.getPassword();
    String confirmPassword = signUpRequestDto.getConfirmPassword();
    var userRole = signUpRequestDto.getUserRole();

    try {
      userService.register(name, email, password, confirmPassword, userRole);
      return "redirect:/";
    } catch (IllegalArgumentException e) {
      model.addAttribute("errorMessage", e.getMessage());
      model.addAttribute("signUpRequestDto", signUpRequestDto);
      return "register";
    }
  }
}
