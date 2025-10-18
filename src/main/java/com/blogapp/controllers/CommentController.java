package com.blogapp.controllers;

import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Comment;
import com.blogapp.models.Post;
import com.blogapp.models.Role;
import com.blogapp.models.User;
import com.blogapp.security.CustomUserDetails;
import com.blogapp.services.CommentService;
import com.blogapp.services.PostService;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/posts/{postId}/comments")
public class CommentController {

  private final CommentService commentService;
  private final PostService postService;

  @Autowired
  public CommentController(CommentService commentService, PostService postService) {
    this.commentService = commentService;
    this.postService = postService;
  }

  @GetMapping
  public String getComments(
      @PathVariable Long postId,
      Model model,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    // Fetch the post
    Optional<Post> optionalPost = postService.getPost(postId);
    if (optionalPost.isEmpty()) {
      throw new NoPostException("Post with the id " + postId + " doesn't exist!", postId);
    }

    // Fetch comments
    Set<Comment> comments = commentService.getCommentsByPostId(postId);

    model.addAttribute("post", optionalPost.get());
    model.addAttribute("comments", comments);
    model.addAttribute("showComments", true);
    model.addAttribute("canManageComments", canManageComments(optionalPost.get(), userDetails));
    return "post";
  }

  @GetMapping("/{commentId}")
  public String getCommentById(
      @PathVariable Long postId,
      @PathVariable Long commentId,
      Model model,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    // Verify post exists
    Optional<Post> optionalPost = postService.getPost(postId);
    if (optionalPost.isEmpty()) {
      throw new NoPostException("Post with the id " + postId + " doesn't exist!", postId);
    }

    // Fetch the specific comment
    Comment comment = commentService.getCommentById(commentId);
    if (comment == null) {
      throw new RuntimeException("Comment with id " + commentId + " not found!");
    }

    model.addAttribute("post", optionalPost.get());
    model.addAttribute("comment", comment);
    model.addAttribute("canManageComments", canManageComments(optionalPost.get(), userDetails));
    return "comment-detail"; // You'd need to create this view
  }

  @GetMapping("/new")
  public String showNewCommentForm(
      @PathVariable Long postId,
      Model model,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    // Fetch the post
    Optional<Post> optionalPost = postService.getPost(postId);
    if (optionalPost.isEmpty()) {
      throw new NoPostException("Post with the id " + postId + " doesn't exist!", postId);
    }
    Post post = optionalPost.get();
    // Load all existing comments so they remain visible
    Set<Comment> comments = commentService.getCommentsByPostId(postId);

    model.addAttribute("post", post);
    model.addAttribute("comments", comments);
    model.addAttribute("showComments", true);
    // Signal Thymeleaf to render the add-comment form inline
    model.addAttribute("addComment", true);
    // Prepare empty comment object for the form
    model.addAttribute("comment", new Comment());
    model.addAttribute("canManageComments", canManageComments(post, userDetails));
    return "post";
  }

  @PostMapping
  public String handleNewCommentSubmission(
      @PathVariable Long postId,
      @ModelAttribute Comment comment,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    // Save the new comment (commentator name, email and content are bound from form)
    User currentUser = extractUser(userDetails);
    if (currentUser != null) {
      comment.setCommentWriterName(currentUser.getName());
      comment.setEmail(currentUser.getEmail());
    }
    commentService.saveComment(comment, postId, currentUser);
    // Redirect to show comments for this post
    return "redirect:/posts/" + postId + "/comments";
  }

  @GetMapping("/{commentId}/edit")
  public String showEditCommentForm(
      @PathVariable Long postId,
      @PathVariable Long commentId,
      Model model,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    // Fetch the post
    Post post =
        postService
            .getPost(postId)
            .orElseThrow(
                () ->
                    new NoPostException("Post with the id " + postId + " doesn't exist!", postId));
    if (!canManageComments(post, userDetails)) {
      throw new AccessDeniedException("You are not allowed to manage comments for this post");
    }
    // Fetch existing comments
    Set<Comment> comments = commentService.getCommentsByPostId(postId);

    // Fetch the specific comment for editing
    Comment existingComment = commentService.getCommentById(commentId);
    if (existingComment == null) {
      throw new RuntimeException("Comment with id " + commentId + " not found!");
    }
    if (!existingComment.getPost().getId().equals(postId)) {
      throw new NoPostException("Comment does not belong to the specified post", postId);
    }

    model.addAttribute("post", post);
    model.addAttribute("comments", comments);
    model.addAttribute("showComments", true);
    // Mark which comment is being edited
    model.addAttribute("editCommentId", commentId);
    // Bind the existing comment to form
    model.addAttribute("commentForm", existingComment);
    model.addAttribute("canManageComments", true);
    return "post";
  }

  @PatchMapping("/{commentId}")
  public String editComment(
      @PathVariable Long postId,
      @PathVariable Long commentId,
      @ModelAttribute("commentForm") Comment updatedComment,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Post post =
        postService
            .getPost(postId)
            .orElseThrow(
                () -> new NoPostException("Post with the id " + postId + " doesn't exist!", postId));
    if (!canManageComments(post, userDetails)) {
      throw new AccessDeniedException("You are not allowed to manage comments for this post");
    }
    Comment existingComment = commentService.getCommentById(commentId);
    if (existingComment == null || !existingComment.getPost().getId().equals(postId)) {
      throw new RuntimeException("Comment with id " + commentId + " not found!");
    }
    // Update only the content
    commentService.updateCommentContent(commentId, updatedComment.getCommentContent());

    return "redirect:/posts/" + postId + "/comments";
  }

  @DeleteMapping("/{commentId}")
  public String deleteComment(
      @PathVariable Long postId,
      @PathVariable Long commentId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    // perform soft-delete via service
    Post post =
        postService
            .getPost(postId)
            .orElseThrow(
                () -> new NoPostException("Post with the id " + postId + " doesn't exist!", postId));
    if (!canManageComments(post, userDetails)) {
      throw new AccessDeniedException("You are not allowed to manage comments for this post");
    }
    Comment existingComment = commentService.getCommentById(commentId);
    if (existingComment == null || !existingComment.getPost().getId().equals(postId)) {
      throw new RuntimeException("Comment with id " + commentId + " not found!");
    }
    commentService.deleteComment(commentId);
    // redirect back to comments view
    return "redirect:/posts/" + postId + "/comments";
  }

  private boolean canManageComments(Post post, CustomUserDetails userDetails) {
    User user = extractUser(userDetails);
    if (user == null) {
      return false;
    }
    if (user.getUserRole() == Role.ADMIN) {
      return true;
    }
    return post.getAuthor() != null && post.getAuthor().getId().equals(user.getId());
  }

  private User extractUser(CustomUserDetails userDetails) {
    if (userDetails == null) {
      return null;
    }
    return userDetails.getUser();
  }
}
