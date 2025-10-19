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
public class SignUpResponseDto {
  private Long id;
  private String name;
  private String email;
  private Role role;
  private String message;
}
