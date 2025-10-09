package com.blogapp.services;

import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.blogapp.repositories.PostRepository;

import java.time.Instant;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepository postRepository;


    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Optional<Post> getPost(long id){
        return postRepository.findById(id);
    }

    public Page<Post> getAllPosts(int pageNumber, int pageSize){
        return postRepository.findAll(PageRequest.of(pageNumber, pageSize));
    }

//    public Page<Post> getAllPostsCustom(int pageNumber, int pageSize){
////        int pageNumber = start / limit;
//        return postRepository.findAll(PageRequest.of(pageNumber, pageSize));
//    }

    public void deletePost(int id){
        Optional<Post> currentPost = getPost(id);
    }

    public Post savePost(Post post){
        Post savedPost = postRepository.save(post);
        String excerpt = getExcerpt(savedPost.getContent());
        savedPost.setExcerpt(excerpt);
        if (savedPost.getPublishedAt() == null) {
            savedPost.setPublishedAt(Instant.now());
            savedPost.setPublished(true);
        }
        return postRepository.save(savedPost);
    }

    public String getExcerpt(String content) {
        if (content == null || content.isEmpty()) return "";
        String[] sentences = content.split("(?<=[.!?])\\s+");
        int count = Math.min(2, sentences.length);
        StringBuilder excerpt = new StringBuilder();
        for (int i = 0; i < count; i++) {
            excerpt.append(sentences[i]);
            if (i < count - 1) excerpt.append(" ");
        }
        return excerpt.toString().trim();
    }
}
