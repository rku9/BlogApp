package com.blogapp.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostFormDto {
    private Long id;
    private String title;
    private String content;
    private String tagListString;
}
