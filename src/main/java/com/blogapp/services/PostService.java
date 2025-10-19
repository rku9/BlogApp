package com.blogapp.services;

import com.blogapp.dtos.PostFormDto;
import com.blogapp.dtos.PostParamFilterDto;
import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Post;
import com.blogapp.models.Tag;
import com.blogapp.models.User;
import com.blogapp.repositories.PostRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Service layer handling core post operations. */
@Service
public class PostService {
  private final PostRepository postRepository;
  private final CommentService commentService;
  private final TagService tagService;

  public PostService(
      PostRepository postRepository, @Lazy CommentService commentService, TagService tagService) {
    this.postRepository = postRepository;
    this.commentService = commentService;
    this.tagService = tagService;
  }

  /** Fetch all distinct author names from posts. */
  public Set<String> getDistinctAuthors() {
    return postRepository.findDistinctAuthors();
  }

  /** Search posts by optional author names, tags and text content, with pagination and sorting. */
  public Page<Post> searchPosts(
      List<String> authorNames,
      List<Long> tagIds,
      String searchString,
      Instant fromDate,
      Instant toDate,
      Pageable pageable) {
    long tagCount = tagIds == null ? 0 : tagIds.size();
    return postRepository.findAll(
        authorNames, tagIds, searchString, fromDate, toDate, tagCount, pageable);
  }

  /** Retrieve a post by its identifier. */
  public Optional<Post> getPost(long id) {
    return postRepository.findById(id);
  }

  /** Prepare model attributes for a post view. */
  public String getPost(long id, Model model) {
    Post post =
        getPost(id)
            .orElseThrow(() -> new NoPostException("Post with the id " + id + "doesn't exist!", id));
    model.addAttribute("post", post);
    return "post";
  }

  /** Determine if a keyword change requires redirecting the page. */
  public String resolveRedirectForAllPosts(
      Pageable pageable,
      PostParamFilterDto postParamFilterDto,
      RedirectAttributes redirectAttributes) {
    normalizeFilters(postParamFilterDto);
    return redirectIfKeywordChanged(postParamFilterDto, pageable, redirectAttributes);
  }

  /** Fetch paginated posts honoring filter parameters. */
  public Page<Post> getAllPosts(Pageable pageable, PostParamFilterDto postParamFilterDto) {
    normalizeFilters(postParamFilterDto);

    List<String> sanitizedAuthors = sanitizeAuthors(postParamFilterDto.getAuthorNames());
    List<Long> sanitizedTagIds = sanitizeTagIds(postParamFilterDto.getTagIds());
    postParamFilterDto.setAuthorNames(sanitizedAuthors);
    postParamFilterDto.setTagIds(sanitizedTagIds);
    Instant fromInstant = resolveFromDateToInstant(postParamFilterDto.getFromDate());
    Instant toInstant = resolveToDateToInstant(postParamFilterDto.getToDate());

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

    return searchPosts(
        sanitizedAuthors,
        sanitizedTagIds,
        postParamFilterDto.getSearch(),
        fromInstant,
        toInstant,
        sortedPageable);
  }

  public Set<Tag> getAllTags() {
    return tagService.findAllTags();
  }

  /** Prepare model for the new post form. */
  public String showNewPostForm(Model model) {
    if (!model.containsAttribute("postFormDto")) {
      model.addAttribute("postFormDto", new PostFormDto());
    }
    return "new-post";
  }

  /** Persist a new post and return the redirect location. */
  public String savePost(PostFormDto postFormDto, String tagListString, User author) {
    Post post = new Post();
    post.setTitle(postFormDto.getTitle());
    post.setContent(postFormDto.getContent());
    post.setAuthor(author);
    Post savedPost = savePostWithTags(post, tagListString);
    return "redirect:/posts/" + savedPost.getId();
  }

  /** Prepare model for the edit post form. */
  public String showEditPostForm(Long id, Model model) {
    Post post =
        getPost(id)
            .orElseThrow(
                () -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

    PostFormDto postFormDto = new PostFormDto();
    postFormDto.setId(post.getId());
    postFormDto.setTitle(post.getTitle());
    postFormDto.setContent(post.getContent());
    postFormDto.setTagListString(post.convertSetOfTagToString(post.getTags()));
    if (post.getAuthor() != null) {
      postFormDto.setAuthorId(post.getAuthor().getId());
    }

    model.addAttribute("postFormDto", postFormDto);
    return "new-post";
  }

  /** Update an existing post with provided form data. */
  public String editPost(Long id, PostFormDto postFormDto, User newAuthor) {
    updatePostWithTags(
        id,
        postFormDto.getTitle(),
        postFormDto.getContent(),
        postFormDto.getTagListString(),
        newAuthor);
    return "redirect:/posts/" + id;
  }

  /** Delete a post and return the redirect location. */
  public String deletePost(Long id) {
    deletePostAndCleanup(id);
    return "redirect:/";
  }

  /** Persist a post, enriching it with excerpt, author and published timestamp if needed. */
  public Post savePost(Post post) {
    String excerpt = getExcerpt(post.getContent());
    post.setExcerpt(excerpt);
    if (post.getAuthor() == null) {
      throw new IllegalArgumentException("Author must be set before saving the post");
    }
    if (post.getPublishedAt() == null) {
      post.setPublishedAt(Instant.now());
      post.setPublished(true);
    }
    return postRepository.save(post);
  }

  /** Generate a short excerpt from the supplied post content. */
  public String getExcerpt(String content) {
    if (content == null || content.isEmpty()) {
      return "";
    }
    String[] sentences = content.split("(?<=[.!?])\\s+");
    int count = Math.min(2, sentences.length);
    StringBuilder excerpt = new StringBuilder();
    for (int i = 0; i < count; i++) {
      excerpt.append(sentences[i]);
      if (i < count - 1) {
        excerpt.append(" ");
      }
    }
    return excerpt.toString().trim();
  }

  /** Saves a post with its tags parsed from a comma-separated list. */
  @Transactional
  public Post savePostWithTags(Post post, String tagListString) {
    Set<Tag> tags = tagService.saveTags(tagListString);
    for (Tag tag : tags) {
      tag.getPosts().add(post);
    }
    post.setTags(tags);
    return savePost(post);
  }

  /** Update an existing post and reconcile its associated tags. */
  @Transactional
  public void updatePostWithTags(
      Long postId, String title, String content, String tagListString, User newAuthor) {
    Post existingPost =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new NoPostException("Post not found", postId));

    Set<Tag> oldTags = existingPost.getTags();
    existingPost.setTitle(title);
    existingPost.setContent(content);
    if (newAuthor != null) {
      existingPost.setAuthor(newAuthor);
    }
    Set<Tag> tags = tagService.saveTags(tagListString);
    existingPost.setTags(tags);

    for (Tag tag : tags) {
      tag.getPosts().add(existingPost);
    }

    tagService.deleteUnusedTags(oldTags);
    postRepository.save(existingPost);
  }

  /** Hard-delete a post and its comments. */
  @Transactional
  public void deletePostAndCleanup(Long id) {
    Post post =
        postRepository
            .findById(id)
            .orElseThrow(() -> new NoPostException("Post not found with id: " + id, id));
    // Delete unused tags before deleting post
    tagService.deleteUnusedTags(post.getTags());
    // Hard delete the post (cascade will auto-delete comments)
    postRepository.delete(post);
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
    List<String> cleaned = new ArrayList<>();
    for (String authorName : authorNames) {
      if (authorName == null) {
        continue;
      }
      String trimmed = authorName.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      cleaned.add(trimmed);
    }
    if (cleaned.isEmpty()) {
      return null;
    }
    return cleaned;
  }

  private List<Long> sanitizeTagIds(List<Long> tagIds) {
    if (tagIds == null) {
      return null;
    }
    List<Long> cleaned = new ArrayList<>();
    for (Long tagId : tagIds) {
      if (tagId != null) {
        cleaned.add(tagId);
      }
    }
    if (cleaned.isEmpty()) {
      return null;
    }
    return cleaned;
  }

  private Instant resolveFromDateToInstant(LocalDate fromDate) {
    if (fromDate == null) {
      return null;
    }
    return fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  private Instant resolveToDateToInstant(LocalDate toDate) {
    if (toDate == null) {
      return null;
    }
    return toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
  }
}
