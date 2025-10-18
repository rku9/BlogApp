package com.blogapp.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Comment extends BaseModel {

  @Column(nullable = false)
  private String commentWriterName;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String commentContent;

  @ManyToOne
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
