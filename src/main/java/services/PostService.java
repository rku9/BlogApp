package services;

import exceptions.NoPostException;
import models.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import repositories.PostRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository){
        this.postRepository = postRepository;
    }

    public Post getPost(long id){
        Optional<Post> optionalPost = postRepository.findById(id);
        if(optionalPost.isEmpty()){
            throw new NoPostException("Post with the id "+id+"doesn't exist!", id);
        }
        return optionalPost.get();
    }

    public List<Post> getAllPosts(int start, int limit){
        return postRepository.getAllPosts(start, limit);
    }

    public void deletePost(int id){
        postRepository.deleteById(id);
    }

    public Post addPost(Post post){
        return postRepository.save(post);
    }
}
