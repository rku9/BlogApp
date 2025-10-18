package com.blogapp.api.controllers;

import com.blogapp.dtos.CommentRequestDto;
import com.blogapp.dtos.CommentResponseDto;
import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Comment;
import com.blogapp.models.Post;
import com.blogapp.models.Role;
import com.blogapp.models.User;
import com.blogapp.security.CustomUserDetails;
import com.blogapp.services.CommentService;
import com.blogapp.services.PostService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("apiCommentController")
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;

    @Autowired
    public CommentController(CommentService commentService, PostService postService) {
        this.commentService = commentService;
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable Long postId) {
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new NoPostException("Post with the id " + postId + " doesn't exist!", postId));

        Set<Comment> comments = commentService.getCommentsByPostId(postId);
        List<CommentResponseDto> response = comments.stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> getComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new NoPostException("Post with the id " + postId + " doesn't exist!", postId));

        Comment comment = commentService.getCommentById(commentId);
        if (comment == null || !comment.getPost().getId().equals(post.getId())) {
            throw new NoPostException("Comment with id " + commentId + " not found for the specified post", commentId);
        }

        return ResponseEntity.ok(toCommentResponse(comment));
    }

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long postId,
            @RequestBody CommentRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Post post = postService.getPost(postId)
                .orElseThrow(() -> new NoPostException("Post with the id " + postId + " doesn't exist!", postId));

        User currentUser = userDetails != null ? userDetails.getUser() : null;

        Comment comment = new Comment();
        comment.setCommentContent(request.getCommentContent());
        if (currentUser != null) {
            comment.setCommentWriterName(currentUser.getName());
            comment.setEmail(currentUser.getEmail());
        } else {
            comment.setCommentWriterName(request.getCommentWriterName());
            comment.setEmail(request.getEmail());
        }

        Comment saved = commentService.saveComment(comment, postId, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCommentResponse(saved));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> editComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Post post = postService.getPost(postId)
                .orElseThrow(() -> new NoPostException("Post with the id " + postId + " doesn't exist!", postId));

        if (!canManageComments(post, userDetails)) {
            throw new AccessDeniedException("You are not allowed to manage comments for this post");
        }

        Comment existingComment = commentService.getCommentById(commentId);
        if (existingComment == null || !existingComment.getPost().getId().equals(postId)) {
            throw new NoPostException("Comment with id " + commentId + " not found for the specified post", commentId);
        }

        commentService.updateCommentContent(commentId, request.getCommentContent());
        Comment updated = commentService.getCommentById(commentId);
        return ResponseEntity.ok(toCommentResponse(updated));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Post post = postService.getPost(postId)
                .orElseThrow(() -> new NoPostException("Post with the id " + postId + " doesn't exist!", postId));

        if (!canManageComments(post, userDetails)) {
            throw new AccessDeniedException("You are not allowed to manage comments for this post");
        }

        Comment existingComment = commentService.getCommentById(commentId);
        if (existingComment == null || !existingComment.getPost().getId().equals(postId)) {
            throw new NoPostException("Comment with id " + commentId + " not found for the specified post", commentId);
        }

        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    private boolean canManageComments(Post post, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        User user = userDetails.getUser();
        if (user == null) {
            return false;
        }
        if (user.getUserRole() == Role.ADMIN) {
            return true;
        }
        return post.getAuthor() != null && post.getAuthor().getId().equals(user.getId());
    }

    private CommentResponseDto toCommentResponse(Comment comment) {
        return new CommentResponseDto(
                comment.getId(),
                comment.getCommentContent(),
                comment.getCommentWriterName(),
                comment.getEmail(),
                comment.getCreatedAt());
    }
}

