package com.blogapp.controllers;

import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Comment;
import com.blogapp.models.Post;
import com.blogapp.services.CommentService;
import com.blogapp.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/post/{postId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;

    @Autowired
    public CommentController(CommentService commentService, PostService postService) {
        this.commentService = commentService;
        this.postService = postService;
    }

    @GetMapping
    public String getComments(@PathVariable Long postId, Model model) {
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
        return "post";
    }

    @GetMapping("/{commentId}")
    public String getCommentById(@PathVariable Long postId,
                                 @PathVariable Long commentId,
                                 Model model) {
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
        return "comment-detail"; // You'd need to create this view
    }

    @GetMapping("/newcomment")
    public String showNewCommentForm(@PathVariable Long postId, Model model) {
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
        return "post";
    }

    @PostMapping("/newcomment")
    public String handleNewCommentSubmission(@PathVariable Long postId,
                                            @ModelAttribute Comment comment) {
        // Implementation to save comment
        //post id needs to be passed to make sure the cascade sync happens
        //by first saving the post and then the comment.
        //comment object is the owner of the relationship.

        Comment savedComment = commentService.saveComment(comment, postId);
        return "redirect:/post/" + postId + "/comments";
    }

    @GetMapping("/editcomment/{commentId}")
    public String showEditCommentForm(@PathVariable Long postId,
                                      @PathVariable Long commentId,
                                      Model model) {
        // Fetch the post
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new NoPostException("Post with the id " + postId + " doesn't exist!", postId));
        // Fetch existing comments
        Set<Comment> comments = commentService.getCommentsByPostId(postId);

        // Fetch the specific comment for editing
        Comment existingComment = commentService.getCommentById(commentId);
        if (existingComment == null) {
            throw new RuntimeException("Comment with id " + commentId + " not found!");
        }

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("showComments", true);
        // Mark which comment is being edited
        model.addAttribute("editCommentId", commentId);
        // Bind the existing comment to form
        model.addAttribute("commentForm", existingComment);
        return "post";
    }

    @PatchMapping("/editcomment/{commentId}")
    public String editComment(@PathVariable Long postId,
                              @PathVariable Long commentId,
                              @ModelAttribute("commentForm") Comment updatedComment) {
        // Update only the content
        commentService.updateCommentContent(commentId, updatedComment.getCommentContent());

        return "redirect:/post/" + postId + "/comments";

    }


    @DeleteMapping("/deletecomment/{commentId}")
    public String deleteComment(@PathVariable Long postId,
                                @PathVariable Long commentId) {
        // perform soft-delete via service
        commentService.deleteComment(commentId);
        // redirect back to comments view
        return "redirect:/post/" + postId + "/comments";
    }
}
