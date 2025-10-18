package com.blogapp.services;

import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Comment;
import com.blogapp.models.Post;
import com.blogapp.models.User;
import com.blogapp.repositories.CommentRepository;
import jakarta.transaction.Transactional;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class CommentService {
  private final CommentRepository commentRepository;
  private final PostService postService; // use service to fetch Post

  public CommentService(CommentRepository commentRepository, @Lazy PostService postService) {
    this.commentRepository = commentRepository;
    this.postService = postService;
  }

  public Comment getCommentById(Long id) {
    return commentRepository.findById(id).orElse(null);
  }

  public Set<Comment> getCommentsByPostId(Long postId) {
    Post post =
        postService
            .getPost(postId)
            .orElseThrow(() -> new NoPostException("Post not found with id: ", postId));
    return commentRepository.findAllByPostId(postId);
  }

  public Comment saveComment(Comment comment, Long postId, User user) {
    Post post =
        postService
            .getPost(postId)
            .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

    // Set the relationship on the comment side
    comment.setPost(post);
    comment.setUser(user);

    // Optional: Keep in-memory collection synchronized (if applicable)
    if (post.getComments() != null) {
      post.getComments().add(comment);
    }
    return commentRepository.save(comment);
  }

  /** Hard-delete a single comment by its ID. */
  @Transactional
  public void deleteComment(Long commentId) {
    Comment comment = getCommentById(commentId);
    if (comment != null) {
      commentRepository.delete(comment);
    }
  }

  /** Hard-delete all comments for a given post. */
  @Transactional
  public void deleteCommentsByPostId(Long postId) {
    Set<Comment> comments = getCommentsByPostId(postId);
    commentRepository.deleteAll(comments);
  }

  /** Update the content of an existing comment. */
  @Transactional
  public void updateCommentContent(Long commentId, String content) {
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
    comment.setCommentContent(content);
    commentRepository.save(comment);
  }
}
