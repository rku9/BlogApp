package com.blogapp.repositories;

import com.blogapp.models.Comment;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
  Set<Comment> findAllByPostId(Long postId);
}
