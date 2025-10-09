package com.blogapp.models;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@AttributeOverride(name = "id", column = @Column(name = "post_id"))
public class Post extends BaseModel{
   private String title;
   private String excerpt;
   
   @Column(columnDefinition = "TEXT")
   private String content;
   private String author;


   private Instant publishedAt;
   private boolean isPublished;
}
