package com.blogapp.services;

import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.blogapp.repositories.PostRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    private final TagService tagService;
    private final PostTagService postTagService;
    private final PostRepository postRepository;


    public PostService(TagService tagService, PostTagService postTagService,
                       PostRepository postRepository){
        this.tagService = tagService;
        this.postTagService = postTagService;
        this.postRepository = postRepository;
    }

    public Post getPost(long id){
        Optional<Post> optionalPost = postRepository.findById(id);
        if(optionalPost.isEmpty()){
            throw new NoPostException("Post with the id "+id+"doesn't exist!", id);
        }
        return optionalPost.get();
    }

    public Page<Post> getAllPosts(int pageNumber, int pageSize){
        return postRepository.findAll(PageRequest.of(pageNumber, pageSize));
    }

    public Page<Post> getAllPostsCustom(int pageNumber, int pageSize){
//        int pageNumber = start / limit;
        return postRepository.findAll(PageRequest.of(pageNumber, pageSize));
    }

    public void deletePost(int id){
        postRepository.deleteById(id);
    }

    public Post savePost(Post post){

        Post savedPost = postRepository.save(post);
        //the post is saved to the db with all the atts set to their value or null.


        return savedPost;
    }
}
