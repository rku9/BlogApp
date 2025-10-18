package com.blogapp.controllers;

import com.blogapp.dtos.*;
import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.*;
import com.blogapp.security.CustomUserDetails;
import com.blogapp.services.*;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
public class PostController {

  private final PostService postService;
  private final UserService userService;

  @Autowired
  public PostController(PostService postService, UserService userService) {
    this.postService = postService;
    this.userService = userService;
  }

  @GetMapping("/posts/{id}")
  public String getPost(
      @PathVariable long id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
    Post post =
        postService
            .getPost(id)
            .orElseThrow(() -> new NoPostException("Post with the id " + id + " doesn't exist!", id));
    model.addAttribute("post", post);

    boolean canEdit = false;
    if (userDetails != null) {
      boolean isAdmin = userDetails.getUser().getUserRole() == Role.ADMIN;
      boolean isAuthor =
          post.getAuthor() != null
              && post.getAuthor().getId().equals(userDetails.getUser().getId());
      canEdit = isAdmin || isAuthor;
      model.addAttribute("currentUserName", userDetails.getName());
    }
    model.addAttribute("canEditPost", canEdit);
    return "post";
  }

  /** Render paginated list of posts honoring optional filter parameters. */
  @GetMapping
  public String getAllPosts(
      @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      @ModelAttribute PostParamFilterDto postParamFilterDto,
      RedirectAttributes redirectAttributes,
      Model model) {
    String redirect =
        postService.resolveRedirectForAllPosts(pageable, postParamFilterDto, redirectAttributes);
    if (redirect != null) {
      return redirect;
    }

    Page<Post> postPage = postService.getAllPosts(pageable, postParamFilterDto);
    Set<String> authors = postService.getDistinctAuthors();
    Set<Tag> allTags = postService.getAllTags();

    model.addAttribute("allPosts", postPage.getContent());
    model.addAttribute("page", postPage);
    model.addAttribute("authorOptions", authors);
    model.addAttribute("selectedAuthors", postParamFilterDto.getAuthorNames());
    model.addAttribute("allTags", allTags);
    model.addAttribute("selectedTagIds", postParamFilterDto.getTagIds());
    model.addAttribute("search", postParamFilterDto.getSearch());
    model.addAttribute("fromDate", postParamFilterDto.getFromDate());
    model.addAttribute("toDate", postParamFilterDto.getToDate());
    model.addAttribute("pageable", postPage.getPageable());
    model.addAttribute("direction", postParamFilterDto.getDirection());
    model.addAttribute("sort", "publishedAt");
    model.addAttribute("oldSearch", postParamFilterDto.getOldSearch());
    model.addAttribute("filters", postParamFilterDto);

    return "all-posts";
  }

  /** Present a blank form for creating a new post. */
  @GetMapping("/posts/new")
  public String showNewPostForm(
      Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) {
      return "redirect:/login";
    }

    PostFormDto postFormDto = (PostFormDto) model.getAttribute("postFormDto");
    if (postFormDto == null) {
      postFormDto = new PostFormDto();
      postFormDto.setAuthorId(userDetails.getUser().getId());
      model.addAttribute("postFormDto", postFormDto);
    }

    model.addAttribute("authorName", userDetails.getName());
    boolean canEditAuthor = userDetails.getUser().getUserRole() == Role.ADMIN;
    model.addAttribute("canEditAuthor", canEditAuthor);
    if (canEditAuthor) {
      List<User> users = userService.findAllUsers();
      model.addAttribute("allAuthors", users);
    }

    return postService.showNewPostForm(model);
  }

  /** Persist a newly created post with associated tags. */
  @PostMapping("/posts")
  public String savePost(
      @ModelAttribute PostFormDto postFormDto,
      @RequestParam("tagListString") String tagListString,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) {
      return "redirect:/login";
    }

    User author = resolveAuthorForRequest(postFormDto, userDetails);
    return postService.savePost(postFormDto, tagListString, author);
  }

  /** Present the edit form populated with an existing post. */
  @GetMapping("/posts/{id}/edit")
  public String showEditPostForm(
      @PathVariable Long id,
      Model model,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) {
      return "redirect:/login";
    }

    Post post =
        postService
            .getPost(id)
            .orElseThrow(() -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

    boolean isAdmin = userDetails.getUser().getUserRole() == Role.ADMIN;
    boolean isAuthor =
        post.getAuthor() != null && post.getAuthor().getId().equals(userDetails.getUser().getId());
    if (!isAdmin && !isAuthor) {
      throw new RuntimeException("You are not allowed to edit this post");
    }

    PostFormDto postFormDto = new PostFormDto();
    postFormDto.setId(post.getId());
    postFormDto.setTitle(post.getTitle());
    postFormDto.setContent(post.getContent());
    postFormDto.setTagListString(post.convertSetOfTagToString(post.getTags()));
    if (post.getAuthor() != null) {
      postFormDto.setAuthorId(post.getAuthor().getId());
      model.addAttribute("authorName", post.getAuthor().getName());
    } else {
      model.addAttribute("authorName", "");
    }
    model.addAttribute("postFormDto", postFormDto);
    model.addAttribute("canEditAuthor", isAdmin);
    if (isAdmin) {
      List<User> users = userService.findAllUsers();
      model.addAttribute("allAuthors", users);
    }

    return postService.showNewPostForm(model);
  }

  /** Apply updates to an existing post. */
  @PatchMapping("/posts/{id}")
  public String editPost(
      @PathVariable Long id,
      @ModelAttribute PostFormDto postFormDto,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) {
      return "redirect:/login";
    }

    Post post =
        postService
            .getPost(id)
            .orElseThrow(() -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

    boolean isAdmin = userDetails.getUser().getUserRole() == Role.ADMIN;
    boolean isAuthor =
        post.getAuthor() != null && post.getAuthor().getId().equals(userDetails.getUser().getId());
    if (!isAdmin && !isAuthor) {
      throw new RuntimeException("You are not allowed to edit this post");
    }

    User authorToSet = post.getAuthor();
    if (isAdmin && postFormDto.getAuthorId() != null) {
      authorToSet =
          userService
              .findById(postFormDto.getAuthorId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "User not found with id: " + postFormDto.getAuthorId()));
    }

    return postService.editPost(id, postFormDto, authorToSet);
  }

  /** Delete a post and redirect to the list page. */
  @DeleteMapping("/posts/{id}")
  public String deletePost(
      @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) {
      return "redirect:/login";
    }

    Post post =
        postService
            .getPost(id)
            .orElseThrow(() -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

    boolean isAdmin = userDetails.getUser().getUserRole() == Role.ADMIN;
    boolean isAuthor =
        post.getAuthor() != null && post.getAuthor().getId().equals(userDetails.getUser().getId());
    if (!isAdmin && !isAuthor) {
      throw new RuntimeException("You are not allowed to delete this post");
    }

    return postService.deletePost(id);
  }

  private User resolveAuthorForRequest(PostFormDto postFormDto, CustomUserDetails userDetails) {
    User currentUser = userDetails.getUser();
    if (currentUser.getUserRole() == Role.ADMIN && postFormDto.getAuthorId() != null) {
      return userService
          .findById(postFormDto.getAuthorId())
          .orElseThrow(
              () ->
                  new IllegalArgumentException(
                      "User not found with id: " + postFormDto.getAuthorId()));
    }

    postFormDto.setAuthorId(currentUser.getId());
    return currentUser;
  }
}
