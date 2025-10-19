package com.blogapp.api.controllers;

import com.blogapp.dtos.CommentResponseDto;
import com.blogapp.dtos.PostFormDto;
import com.blogapp.dtos.PostResponseDto;
import com.blogapp.dtos.TagResponseDto;
import com.blogapp.dtos.UserResponseDto;
import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Post;
import com.blogapp.models.Role;
import com.blogapp.models.User;
import com.blogapp.security.CustomUserDetails;
import com.blogapp.services.PostService;
import com.blogapp.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController("apiPostController")
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    @Autowired
    public PostController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable long id) {
        ResponseEntity<PostResponseDto> responseEntity;
        try {
            Post post =
                    postService
                            .getPost(id)
                            .orElseThrow(() -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

            responseEntity = new ResponseEntity<>(toPostResponse(post), HttpStatus.OK);
        } catch (Exception e) {
            responseEntity = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return responseEntity;
    }

    @GetMapping
    public Page<PostResponseDto> getAllPosts(
            @RequestParam(name = "start", required = false) Integer start,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "authorId", required = false) Long authorId,
            @RequestParam(name = "tagId", required = false) List<Long> tagIds,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "search", required = false) String search) {

        int resolvedLimit = (limit != null && limit > 0) ? limit : 10;
        int resolvedStart = (start != null && start > 0) ? start : 1;
        int pageNumber = (resolvedStart - 1) / resolvedLimit;

        Sort.Direction direction = "ASC".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "publishedAt");
        PageRequest pageable = PageRequest.of(pageNumber, resolvedLimit, sort);

        List<String> authorNames = null;
        if (authorId != null) {
            User author =
                    userService
                            .findById(authorId)
                            .orElseThrow(
                                    () ->
                                            new ResponseStatusException(
                                                    HttpStatus.BAD_REQUEST,
                                                    "Author not found with id: " + authorId));
            authorNames = List.of(author.getName());
        }

        List<Long> sanitizedTagIds = null;
        if (tagIds != null && !tagIds.isEmpty()) {
            sanitizedTagIds = tagIds.stream().filter(id -> id != null).distinct().collect(Collectors.toList());
            if (sanitizedTagIds.isEmpty()) {
                sanitizedTagIds = null;
            }
        }

        String sanitizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : "";

        Page<Post> page =
                postService.searchPosts(authorNames, sanitizedTagIds, sanitizedSearch, null, null, pageable);
        return page.map(this::toPostResponse);
    }

    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(@RequestBody PostFormDto postFormDto,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        User author = resolveAuthorForRequest(postFormDto, userDetails);
        Post post = new Post();
        post.setTitle(postFormDto.getTitle());
        post.setContent(postFormDto.getContent());
        post.setAuthor(author);

        Post saved =
                postFormDto.getTagListString() == null
                        ? postService.savePost(post)
                        : postService.savePostWithTags(post, postFormDto.getTagListString());

        return ResponseEntity.status(HttpStatus.CREATED).body(toPostResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PostResponseDto> updatePost(@PathVariable long id,
                                                      @RequestBody PostFormDto postFormDto,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        Post post =
                postService
                        .getPost(id)
                        .orElseThrow(() -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

        boolean isAdmin = userDetails.getUser().getUserRole() == Role.ADMIN;
        boolean isAuthor =
                post.getAuthor() != null && post.getAuthor().getId().equals(userDetails.getUser().getId());
        if (!isAdmin && !isAuthor) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

        String tagListString = postFormDto.getTagListString();
        if (tagListString == null) {
            tagListString = post.convertSetOfTagToString(post.getTags());
        }

        String titleToSet = postFormDto.getTitle() != null ? postFormDto.getTitle() : post.getTitle();
        String contentToSet = postFormDto.getContent() != null ? postFormDto.getContent() : post.getContent();

        postService.updatePostWithTags(
                id,
                titleToSet,
                contentToSet,
                tagListString,
                authorToSet);

        Post updated =
                postService.getPost(id)
                        .orElseThrow(() -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

        return ResponseEntity.ok(toPostResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable long id,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Post post =
                postService
                        .getPost(id)
                        .orElseThrow(() -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

        boolean isAdmin = userDetails.getUser().getUserRole() == Role.ADMIN;
        boolean isAuthor =
                post.getAuthor() != null && post.getAuthor().getId().equals(userDetails.getUser().getId());
        if (!isAdmin && !isAuthor) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        postService.deletePostAndCleanup(id);
        return ResponseEntity.noContent().build();
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

    private PostResponseDto toPostResponse(Post post) {
        PostResponseDto postResponseDto = new PostResponseDto();
        postResponseDto.setId(post.getId());
        postResponseDto.setTitle(post.getTitle());
        postResponseDto.setExcerpt(post.getExcerpt());
        postResponseDto.setContent(post.getContent());

        List<TagResponseDto> tagDtos =
                post.getTags().stream()
                        .map(tag -> new TagResponseDto(tag.getId(), tag.getName()))
                        .collect(Collectors.toList());
        postResponseDto.setTags(tagDtos);
        postResponseDto.setAuthor(new UserResponseDto(post.getAuthor().getName()));

        List<CommentResponseDto> commentDtos =
                post.getComments().stream()
                        .map(c -> new CommentResponseDto(
                                c.getId(),
                                c.getCommentContent(),
                                c.getCommentWriterName(),
                                c.getEmail(),
                                c.getCreatedAt()))
                        .collect(Collectors.toList());
        postResponseDto.setComments(commentDtos);

        postResponseDto.setPublishedAt(post.getPublishedAt());
        postResponseDto.setPublished(post.isPublished());
        return postResponseDto;
    }
}

