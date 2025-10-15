package com.blogapp.dtos;

import com.blogapp.models.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequestDto {
  private String name;
  private String email;
  private String password;
  private String confirmPassword;
  private Role userRole = Role.AUTHOR;
}
