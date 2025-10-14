package com.blogapp.controllers;

import com.blogapp.dtos.PostFormDto;
import com.blogapp.dtos.PostParamFilterDto;
import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Post;
import com.blogapp.models.Tag;
import com.blogapp.services.PostService;
import com.blogapp.services.TagService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Controller responsible for handling post-related web endpoints. */
@Controller
@RequestMapping("/")
public class PostController {

  private final PostService postService;
  private final TagService tagService;

  @Autowired
  public PostController(PostService postService, TagService tagService) {
    this.postService = postService;
    this.tagService = tagService;
  }

  /** Display a post page for the given identifier. */
  @GetMapping("/post/{id}")
  public String getPost(@PathVariable long id, Model model) {

    Optional<Post> optionalPost = postService.getPost(id);
    if (optionalPost.isEmpty()) {
      throw new NoPostException("Post with the id " + id + "doesn't exist!", id);
    }

    model.addAttribute("post", optionalPost.get());
    return "post";
  }

  /** Render paginated list of posts honoring optional filter parameters. */
  @GetMapping("/")
  public String getAllPosts(
      @PageableDefault(size = 3, sort = "publishedAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      @ModelAttribute PostParamFilterDto postParamFilterDto,
      RedirectAttributes redirectAttributes,
      Model model) {

    normalizeFilters(postParamFilterDto);

    String redirect = redirectIfKeywordChanged(postParamFilterDto, pageable, redirectAttributes);
    if (redirect != null) {
      return redirect;
    }

    List<String> sanitizedAuthors = sanitizeAuthors(postParamFilterDto.getAuthorNames());
    List<Long> sanitizedTagIds = sanitizeTagIds(postParamFilterDto.getTagIds());
    postParamFilterDto.setAuthorNames(sanitizedAuthors);
    postParamFilterDto.setTagIds(sanitizedTagIds);
    Instant fromInstant = resolveFromInstant(postParamFilterDto.getFromDate());
    Instant toInstant = resolveToInstant(postParamFilterDto.getToDate());

    Sort sort = Sort.by("publishedAt");
    if ("ASC".equalsIgnoreCase(postParamFilterDto.getDirection())) {
      sort = sort.ascending();
      postParamFilterDto.setDirection("ASC");
    } else {
      sort = sort.descending();
      postParamFilterDto.setDirection("DESC");
    }

    Pageable sortedPageable =
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

    Page<Post> postPage =
        postService.searchPosts(
            sanitizedAuthors,
            sanitizedTagIds,
            postParamFilterDto.getSearch(),
            fromInstant,
            toInstant,
            sortedPageable);

    Set<String> authors = postService.getDistinctAuthors();
    Set<Tag> allTags = tagService.findAllTags();

    model.addAttribute("allPosts", postPage.getContent());
    model.addAttribute("page", postPage);
    model.addAttribute("authorOptions", authors);
    model.addAttribute("selectedAuthors", postParamFilterDto.getAuthorNames());
    model.addAttribute("allTags", allTags);
    model.addAttribute("selectedTagIds", postParamFilterDto.getTagIds());
    model.addAttribute("search", postParamFilterDto.getSearch());
    model.addAttribute("fromDate", postParamFilterDto.getFromDate());
    model.addAttribute("toDate", postParamFilterDto.getToDate());
    model.addAttribute("pageable", sortedPageable);
    model.addAttribute("direction", postParamFilterDto.getDirection());
    model.addAttribute("sort", "publishedAt");
    model.addAttribute("oldSearch", postParamFilterDto.getOldSearch());
    model.addAttribute("filters", postParamFilterDto);

    return "all-posts";
  }

  private void normalizeFilters(PostParamFilterDto filters) {
    if (filters.getSearch() == null) {
      filters.setSearch("");
    }
    if (filters.getOldSearch() == null) {
      filters.setOldSearch("");
    }
    if (filters.getDirection() == null || filters.getDirection().isBlank()) {
      filters.setDirection("DESC");
    }
    filters.setSort("publishedAt");
  }

  private String redirectIfKeywordChanged(
      PostParamFilterDto filters, Pageable pageable, RedirectAttributes redirectAttributes) {
    if (!Objects.equals(filters.getSearch(), filters.getOldSearch())) {
      redirectAttributes.addAttribute("search", filters.getSearch());
      redirectAttributes.addAttribute("oldSearch", filters.getSearch());
      redirectAttributes.addAttribute("page", 0);
      redirectAttributes.addAttribute("size", pageable.getPageSize());
      redirectAttributes.addAttribute("direction", filters.getDirection());
      return "redirect:/";
    }
    return null;
  }

  private List<String> sanitizeAuthors(List<String> authorNames) {
    if (authorNames == null) {
      return null;
    }
    List<String> cleaned =
        authorNames
            .stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(name -> !name.isEmpty())
            .collect(Collectors.toList());
    return cleaned.isEmpty() ? null : cleaned;
  }

  private List<Long> sanitizeTagIds(List<Long> tagIds) {
    if (tagIds == null) {
      return null;
    }
    List<Long> cleaned = tagIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
    return cleaned.isEmpty() ? null : cleaned;
  }

  private Instant resolveFromInstant(LocalDate fromDate) {
    if (fromDate == null) {
      return null;
    }
    return fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  private Instant resolveToInstant(LocalDate toDate) {
    if (toDate == null) {
      return null;
    }
    return toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  /** Present a blank form for creating a new post. */
  @GetMapping("/newpost")
  public String showNewPostForm(Model model) {
    model.addAttribute("postFormDto", new PostFormDto());
    return "new-post";
  }

  /** Persist a newly created post with associated tags. */
  @PostMapping("/newpost")
  public String savePost(
      @ModelAttribute PostFormDto postFormDto,
      @RequestParam("tagListString") String tagListString) {
    // Save post with tags via PostService
    Post post = new Post();
    post.setTitle(postFormDto.getTitle());
    post.setContent(postFormDto.getContent());
    Post savedPost = postService.savePostWithTags(post, tagListString);

    return "redirect:/post/" + savedPost.getId();
  }

  /** Present the edit form populated with an existing post. */
  @GetMapping("/editpost/{id}")
  public String showEditPostForm(@PathVariable Long id, Model model) {
    Post post =
        postService
            .getPost(id)
            .orElseThrow(
                () -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

    PostFormDto postFormDto = new PostFormDto();
    postFormDto.setId(post.getId());
    postFormDto.setTitle(post.getTitle());
    postFormDto.setContent(post.getContent());
    postFormDto.setTagListString(post.convertSetOfTagToString(post.getTags()));

    model.addAttribute("postFormDto", postFormDto);
    return "new-post";
  }

  /** Apply updates to an existing post. */
  @PatchMapping("/editpost/{id}")
  public String editPost(@PathVariable Long id, @ModelAttribute PostFormDto postFormDto) {
    postService.updatePostWithTags(
        id, postFormDto.getTitle(), postFormDto.getContent(), postFormDto.getTagListString());
    return "redirect:/post/" + id;
  }

  /** Soft delete a post and redirect to the list page. */
  @DeleteMapping("/deletepost/{id}")
  public String deletePost(@PathVariable Long id) {
    postService.deletePostAndCleanup(id);
    return "redirect:/";
  }
}
