package com.blogapp.models;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Post extends BaseModel {
  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String excerpt;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @ManyToMany
  @JoinTable(
      name = "post_tag",
      joinColumns = @JoinColumn(name = "post_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Comment> comments;

  private Instant publishedAt;
  private boolean isPublished;

  public String convertSetOfTagToString(Set<Tag> tagSet) {
    if (tagSet == null || tagSet.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (Tag tag : tagSet) {
      sb.append(tag.getName());
      sb.append(",");
    }
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }
}
