package com.blogapp.repositories;

import com.blogapp.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(value = "select * from post order by post_id limit: limit offset: offset", nativeQuery = true)
    List<Post> getAllPosts(@Param("offset") int offset, @Param("limit") int limit);
    void deleteById(long id);
}
