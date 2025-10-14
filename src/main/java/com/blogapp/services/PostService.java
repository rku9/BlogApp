package com.blogapp.services;
import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Post;
import com.blogapp.models.Tag;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import com.blogapp.repositories.PostRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final CommentService commentService;
    private final TagService tagService;

    public PostService(PostRepository postRepository,
                       @Lazy CommentService commentService,
                       TagService tagService) {
        this.postRepository = postRepository;
        this.commentService = commentService;
        this.tagService = tagService;
    }

    /**
     * Fetch all distinct author names from posts.
     */
    public Set<String> getDistinctAuthors() {
        return postRepository.findDistinctAuthors();
    }

    /**
     * Search posts by optional author names, tags and text content, with pagination and sorting.
     */
    public Page<Post> searchPosts(List<String> authorNames,
                                  List<Long> tagIds,
                                  String searchString,
                                  Instant fromDate,
                                  Instant toDate,
                                  org.springframework.data.domain.Pageable pageable) {
        long tagCount = (tagIds == null ? 0 : tagIds.size());
        return postRepository.findAll(authorNames, tagIds, searchString, fromDate, toDate, tagCount, pageable);
    }

    public Optional<Post> getPost(long id){
        return postRepository.findById(id);
    }

//    public Page<Post> getAllPosts(int pageNumber, int pageSize){
//        return postRepository.findAll(null, null, "", PageRequest.of(pageNumber, pageSize));
//
//    }

    public Post savePost(Post post){
        // Prepare post fields before saving
        String excerpt = getExcerpt(post.getContent());
        post.setExcerpt(excerpt);
        // Temporary author until security is implemented
        post.setAuthor("Rajat");
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
            post.setPublished(true);
        }
        // Save with non-null author
        return postRepository.save(post);
    }

    public String getExcerpt(String content) {
        if (content == null || content.isEmpty()) return "";
        String[] sentences = content.split("(?<=[.!?])\\s+");
        int count = Math.min  (2, sentences.length);
        StringBuilder excerpt = new StringBuilder();
        for (int i = 0; i < count; i++) {
            excerpt.append(sentences[i]);
            if (i < count - 1) excerpt.append(" ");
        }
        return excerpt.toString().trim();
    }

    /**
     * Saves a post with its tags parsed from a comma-separated list.
     */
    @Transactional
    public Post savePostWithTags(Post post, String tagListString) {
        Set<Tag> tags = tagService.saveTags(tagListString);
        for (Tag tag : tags) {
            tag.getPosts().add(post);
        }
        post.setTags(tags);
        return savePost(post);
    }

    @Transactional
    public void updatePostWithTags(Long postId,
                                   String title,
                                   String content,
                                   String tagListString) {

        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new NoPostException("Post not found", postId));

        Set<Tag> oldTags = existingPost.getTags();
        // Update fields
        existingPost.setTitle(title);
        existingPost.setContent(content);

        //temp
        existingPost.setAuthor("Rajat");
        //
        // Process tags
        Set<Tag> tags = tagService.saveTags(tagListString);
        existingPost.setTags(tags);

        // Ensure bidirectional link
        for (Tag tag : tags) {
            tag.getPosts().add(existingPost);
        }

        tagService.deleteUnusedTags(oldTags);
        postRepository.save(existingPost);
    }

    /**
     * Soft-delete a post and its comments.
     */
    @Transactional
    public void deletePostAndCleanup(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NoPostException("Post not found with id: " + id, id));
        // mark post as deleted
        post.setDeleted(true);
        // soft-delete comments via CommentService
        commentService.deleteCommentsByPostId(id);
        postRepository.save(post);
    }
}
