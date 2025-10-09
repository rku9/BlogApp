package com.blogapp.controllers;

import com.blogapp.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
//@RequestMapping("/")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/login")
    public String loginUser() {
        return "redirect:/";
    }

    @PostMapping("/register")
    public String registerUser() {
        return "redirect:/";
    }

}
