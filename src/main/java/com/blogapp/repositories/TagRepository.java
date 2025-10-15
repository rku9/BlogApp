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

  /**
   * Retrieves all tags that are not marked as deleted.
   *
   * @return a list containing non-deleted tags
   */
  List<Tag> findAllByDeletedFalse();

  @Query(
      value =
          "SELECT COUNT(*) "
              + "FROM post_tag pt "
              + "JOIN post p ON pt.post_id = p.id "
              + "WHERE pt.tag_id = :tagId AND p.is_deleted = false",
      nativeQuery = true)
  long countPostsByTagIdAndIsDeletedFalse(@Param("tagId") Long tagId);
}
