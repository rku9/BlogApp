package com.blogapp.services;

import com.blogapp.models.Post;
import com.blogapp.models.PostTag;
import com.blogapp.models.Tag;
import com.blogapp.repositories.PostTagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostTagService {

    private final PostTagRepository postTagRepository;

    public PostTagService(PostTagRepository postTagRepository) {
        this.postTagRepository = postTagRepository;
    }

    public void savePostTag(Post post, List<Tag> tag){
        PostTag postTag = new PostTag();
        postTag.setPost(post);
//        postTag.setTag(tag);
        postTagRepository.save(postTag);
    }
}
