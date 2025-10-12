package com.blogapp.repositories;

import com.blogapp.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Set<Comment> findAllByPostId(Long postId);
}
