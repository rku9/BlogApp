package com.blogapp.controllers;

import com.blogapp.models.Post;
import com.blogapp.models.PostTag;
import com.blogapp.models.Tag;
import com.blogapp.services.PostTagService;
import com.blogapp.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.blogapp.services.PostService;

import java.util.List;

@Controller
@RequestMapping("/")
public class PostController {

    private final PostService postService;
    private final TagService tagService;
    private final PostTagService postTagService;

    @Autowired
    public PostController(PostService postService, TagService tagService,
                          PostTagService postTagService){
        this.postService = postService;
        this.tagService = tagService;
        this.postTagService = postTagService;
    }

    //post1
    @GetMapping("/post{id:\\d+}")
    public String getPost(@PathVariable long id, Model model){

        Post post = postService.getPost(id);
        model.addAttribute("post", post);
        return "post";
    }

    @GetMapping("/")
    public String getAllPosts(@RequestParam(name = "start",
                                defaultValue = "0") int start,
                              @RequestParam(name = "limit", defaultValue =
                                      "2147483647 ") int limit,
                              Model model){
        int pageSize = 6;
        int page = start / pageSize;
        Page<Post> postPage = postService.getAllPosts(page, pageSize);
        model.addAttribute("allPosts", postPage.getContent());
        model.addAttribute("page", postPage);
        return "all-posts";
    }

//    @GetMapping("/")
//    public String getAllPostsCustom(@RequestParam("start") int start, @RequestParam("limit") int limit,
//                              Model model){
//        List<Post> allPostsCustom = postService.getAllPostsCustom(start, limit);
//        model.addAttribute("allPostsCustom", allPostsCustom);
//        return "all-posts";
//    }

    public void deletePost(long id){

    }

    @GetMapping("/newpost")
    public String showNewPostForm(Model model){
        model.addAttribute("post", new Post());
        return "new-post";
    }

    @PostMapping("/newpost")
    public String handleNewPostSubmission(@ModelAttribute Post post,
                                          @RequestParam("tags") String tagListString){
        //pass the tagListString to the tag service, and it will save it to the
        //tag repo and the post tag repo.
        List<Tag> savedTag = tagService.saveTag(tagListString);

        //process the post object.
        Post savedPost = postService.savePost(post);

        //after the last we have the post id and the tag id.
        PostTag savedPostTag = postTagService.savePostTag(savedPost, savedTag);
        return "redirect:/post" + savedPost.getId();
    }
}
