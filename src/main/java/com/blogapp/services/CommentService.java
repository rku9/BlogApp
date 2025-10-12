package com.blogapp.services;

import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Comment;
import com.blogapp.models.Post;
import com.blogapp.repositories.CommentRepository;
import com.blogapp.repositories.PostRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.Set;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;  // use repository instead of service to avoid circular dependency

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    public Comment getCommentById(Long id) {
        return commentRepository.findById(id).orElse(null);
    }

    public Set<Comment> getCommentsByPostId(Long postId) {
        // delegate fetching post existence to repository
        Post post = postRepository.findById(postId)
                       .orElseThrow(() -> new NoPostException("Post not found with id: ", postId));
        return commentRepository.findAllByPostId(postId);
    }

    public Comment saveComment(Comment comment, Long postId) {
        // Fetch the post and associate it with the comment
        Post post = postRepository.findById(postId)
                       .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // Set the relationship on both sides for proper bidirectional sync
        comment.setPost(post);
        comment.setEmail("rajat@abc.com");
        comment.setCommentWriterName("Rajat");

        // Optional: Keep in-memory collection synchronized (useful if post is still in session)
        if (post.getComments() != null) {
            post.getComments().add(comment);
        }
        return commentRepository.save(comment);
    }

    /**
     * Soft-delete a single comment by its ID.
     */
    @Transactional
    public void deleteComment(Long commentId) {
        // Mark comment deleted
        Comment comment = getCommentById(commentId);
        if (comment != null) {
            comment.setDeleted(true);
            commentRepository.save(comment);
        }
    }

    /**
     * Soft-delete all comments for a given post.
     */
    @Transactional
    public void deleteCommentsByPostId(Long postId) {
        Set<Comment> comments = getCommentsByPostId(postId);
        comments.forEach(c -> c.setDeleted(true));
        commentRepository.saveAll(comments);
    }

    /**
     * Update the content of an existing comment.
     */
    @Transactional
    public Comment updateCommentContent(Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
        comment.setCommentContent(content);
        return commentRepository.save(comment);
    }
}
