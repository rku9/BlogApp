package com.blogapp.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Tag extends BaseModel {
    private String name;
}
