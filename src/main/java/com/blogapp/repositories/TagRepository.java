package com.blogapp.repositories;

import com.blogapp.models.Tag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

  Tag findByName(String name);

  @Query(
      value =
          "SELECT COUNT(*) "
              + "FROM post_tag pt "
              + "WHERE pt.tag_id = :tagId",
      nativeQuery = true)
  long countPostsByTagId(@Param("tagId") Long tagId);
}
