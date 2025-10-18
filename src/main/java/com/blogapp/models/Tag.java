package com.blogapp.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Tag extends BaseModel implements Comparable<Tag> {
  @Column(unique = true, nullable = false)
  private String name;

  @ManyToMany(mappedBy = "tags")
  private Set<Post> posts = new HashSet<>();

  @Override
  public int compareTo(Tag other) {
    return this.name.compareTo(other.name);
  }
}
