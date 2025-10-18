package com.blogapp.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class PostResponseDto {
    private long id;
    private String title;
    private String excerpt;
    private String content;
    private List<TagResponseDto> tags;
    private UserResponseDto author;
    private List<CommentResponseDto> comments;
    private Instant publishedAt;
    private boolean isPublished;
}
