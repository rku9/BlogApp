package com.blogapp.dtos;

import com.blogapp.models.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
  private Long id;
  private String name;
  private String email;
  private Role userRole;
  private boolean success;
  private String message;
}
