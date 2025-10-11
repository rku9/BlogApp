package com.blogapp.controllers;

import com.blogapp.exceptions.NoPostException;
import com.blogapp.models.Post;
import com.blogapp.services.PostTagManagerService;
import com.blogapp.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.blogapp.services.PostService;

import java.util.Optional;

@Controller
@RequestMapping("/")
public class PostController {

    private final PostService postService;
    private final PostTagManagerService postTagManagerService;
    private final TagService tagService;

    @Autowired
    public PostController(PostService postService,
                          PostTagManagerService postTagManagerService,
                          TagService tagService) {
        this.postService = postService;
        this.postTagManagerService = postTagManagerService;
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
    public String getAllPosts(@RequestParam(value = "start", defaultValue = "0") int start,
                              Model model){
        int pageSize = 6;
        int page = start / pageSize;
        Page<Post> postPage = postService.getAllPosts(page, pageSize);
        model.addAttribute("allPosts", postPage.getContent());
        model.addAttribute("page", postPage);
        return "all-posts";
    }



    @GetMapping("/newpost")
    public String showNewPostForm(Model model) {
        model.addAttribute("post", new Post());
        return "new-post";
    }

    @PostMapping("/newpost")
    public String handleNewPostSubmission(@ModelAttribute Post post,
                                          @RequestParam("tagListString") String tagListString) {
        // Delegate the creation of post with tags to a single service
        Post savedPost = postTagManagerService.savePostAndTags(post, tagListString);

        return "redirect:/post/" + savedPost.getId();
    }

    @GetMapping("/editpost/{id}")
    public String showEditPostForm(@PathVariable Long id, Model model) {
        Optional<Post> optionalPost = postService.getPost(id);
        if (optionalPost.isEmpty()) {
            throw new NoPostException("Post with the id " + id + " doesn't exist!", id);
        }
        model.addAttribute("post", optionalPost.get());
        return "new-post";
    }





    @DeleteMapping("/deletepost/{id}")
    public String deletePost(@PathVariable Long id) {
        postTagManagerService.deletePostAndUnusedTags(id);
        return "redirect:/";
    }
}


//    @PostMapping("/newpost")
//    public String handleNewPostSubmission(@ModelAttribute Post post,
//                                          @RequestParam("tags") String tagListString){
//        //pass the tagListString to the tag service, and it will save it to the
//        //tag repo and the post tag repo.
//        System.out.println(tagListString);
//        List<Tag> savedTag = tagService.saveTag(tagListString);
//
//        //process the post object.
//        Post savedPost = postService.savePost(post, );
//
//        //after the last we have the post id and the tag id.
//        PostTag savedPostTag = postTagService.savePostTag(savedPost, savedTag);
//        return "redirect:/post" + savedPost.getId();
//    }