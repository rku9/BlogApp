package com.blogapp.controllers;

import com.blogapp.dtos.PostFormDto;
import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Post;
import com.blogapp.models.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

    @GetMapping("/post/{id}")
    public String getPost(@PathVariable long id, Model model) {

        Optional<Post> optionalPost = postService.getPost(id);
        if (optionalPost.isEmpty()) {
            throw new NoPostException("Post with the id "+id+"doesn't exist!", id);
        }

        model.addAttribute("post", optionalPost.get());
        return "post";
    }

    @GetMapping("/")
    public String getAllPosts(
            @PageableDefault(size = 3, sort = "publishedAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(value = "authorNames", required = false) List<String> authorNames,
            @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
            @RequestParam(value = "search", defaultValue = "") String search,
            @RequestParam(value = "oldSearch", defaultValue = "") String oldSearch,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            java.time.LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            java.time.LocalDate toDate,
            RedirectAttributes redirectAttributes,
            Model model) {

        String redirect = redirectIfKeywordChanged(search, oldSearch, pageable, redirectAttributes);
        if (redirect != null) {
            return redirect;
        }

        List<String> sanitizedAuthors = sanitizeAuthors(authorNames);
        List<Long> sanitizedTagIds = sanitizeTagIds(tagIds);
        java.time.Instant fromInstant = resolveFromInstant(fromDate);
        java.time.Instant toInstant = resolveToInstant(toDate);

        Page<Post> postPage = postService.searchPosts(sanitizedAuthors, sanitizedTagIds, search, fromInstant, toInstant, pageable);

        Set<String> authors = postService.getDistinctAuthors();
        Set<Tag> allTags = tagService.findAllTags();

        model.addAttribute("allPosts", postPage.getContent());
        model.addAttribute("page", postPage);
        model.addAttribute("authorOptions", authors);
        model.addAttribute("selectedAuthors", sanitizedAuthors);
        model.addAttribute("allTags", allTags);
        model.addAttribute("selectedTagIds", sanitizedTagIds);
        model.addAttribute("search", search);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("pageable", pageable);
        model.addAttribute("oldSearch", search);

        return "all-posts";
    }

    private String redirectIfKeywordChanged(String search,
                                            String oldSearch,
                                            Pageable pageable,
                                            RedirectAttributes redirectAttributes) {
        if (!Objects.equals(search, oldSearch)) {
            redirectAttributes.addAttribute("search", search);
            redirectAttributes.addAttribute("oldSearch", search);
            redirectAttributes.addAttribute("page", 0);
            redirectAttributes.addAttribute("size", pageable.getPageSize());
            return "redirect:/";
        }
        return null;
    }

    private List<String> sanitizeAuthors(List<String> authorNames) {
        if (authorNames == null) {
            return null;
        }
        List<String> cleaned = authorNames.stream()
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
        List<Long> cleaned = tagIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return cleaned.isEmpty() ? null : cleaned;
    }

    private java.time.Instant resolveFromInstant(java.time.LocalDate fromDate) {
        if (fromDate == null) {
            return null;
        }
        return fromDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
    }

    private java.time.Instant resolveToInstant(java.time.LocalDate toDate) {
        if (toDate == null) {
            return null;
        }
        return toDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
    }

    @GetMapping("/newpost")
    public String showNewPostForm(Model model) {
        model.addAttribute("postFormDto", new PostFormDto());
        return "new-post";
    }

    @PostMapping("/newpost")
    public String savePost(@ModelAttribute PostFormDto postFormDto,
                           @RequestParam("tagListString") String tagListString) {
        // Save post with tags via PostService
        Post post = new Post();
        post.setTitle(postFormDto.getTitle());
        post.setContent(postFormDto.getContent());
        Post savedPost = postService.savePostWithTags(post, tagListString);

        return "redirect:/post/" + savedPost.getId();
    }

    @GetMapping("/editpost/{id}")
    public String showEditPostForm(@PathVariable Long id, Model model) {
        Post post = postService.getPost(id).orElseThrow(() -> new NoPostException("Post with the id " + id + " doesn't exist!", id));

        PostFormDto postFormDto = new PostFormDto();
        postFormDto.setId(post.getId());
        postFormDto.setTitle(post.getTitle());
        postFormDto.setContent(post.getContent());
        postFormDto.setTagListString(post.convertSetOfTagToString(post.getTags()));

        model.addAttribute("postFormDto", postFormDto);
        return "new-post";
    }

@PatchMapping("/editpost/{id}")
public String editPost(@PathVariable Long id,
                       @ModelAttribute PostFormDto postFormDto) {
    postService.updatePostWithTags(id,
            postFormDto.getTitle(),
            postFormDto.getContent(),
            postFormDto.getTagListString());
    return "redirect:/post/" + id;
}

    @DeleteMapping("/deletepost/{id}")
    public String deletePost(@PathVariable Long id) {
        postService.deletePostAndCleanup(id);
        return "redirect:/";
    }
}

/*
http://localhost:8080/?search=&oldSearch=&fromDate=&toDate=&page=0&size=3
http://localhost:8080/?search=&oldSearch=&authorNames=&tagIds=&fromDate=&toDate=&page=1&size=3
 */