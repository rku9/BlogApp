package com.blogapp.services;

import com.blogapp.models.Post;
import com.blogapp.models.Tag;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostCreationService {
    private final PostService postService;
    private final TagService tagService;
    private final PostTagService postTagService;

    public PostCreationService(PostService postService, TagService tagService,
                               PostTagService postTagService) {
        this.postService = postService;
        this.tagService = tagService;
        this.postTagService = postTagService;
    }

    @Transactional
    public Post savePostWithTags(Post post, String tagListString) {
        // 1. Save/fetch tags
        List<Tag> tags = tagService.saveTags(tagListString);

        // 2. Save post
        Post savedPost = postService.savePost(post);

        // 3. Link post and tags
        postTagService.savePostTag(savedPost, tags);

        return savedPost;
    }
}
