package com.blogapp.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Comment extends BaseModel{
    private String name;
    private String email;
    private String comment;
    @ManyToOne
    private Post post;
}
