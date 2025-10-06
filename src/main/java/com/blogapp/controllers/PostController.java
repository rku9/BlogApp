package com.blogapp.controllers;

import com.blogapp.models.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.blogapp.services.PostService;

import java.util.List;

@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService){
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public String getPost(@PathVariable long id, Model model){
        Post post = postService.getPost(id);
        model.addAttribute("post", post);
        return "post";
    }

    @GetMapping("/")
    public List<Post> getAllPosts(@RequestParam("start") int startPosition, @RequestParam("limit") int pageSize){
        return postService.getAllPosts(startPosition, pageSize);
    }

    public void deletePost(long id){

    }
}
