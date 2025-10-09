package com.blogapp.repositories;

import com.blogapp.models.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {


}
