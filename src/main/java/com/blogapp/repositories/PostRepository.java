package com.blogapp.repositories;

import com.blogapp.models.Post;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

  @Query(
      value =
          "select distinct p "
              + "from Post p "
              + "join p.author a "
              + "left join p.tags t "
              + "left join p.comments c "
              + "where ("
              + "lower(p.title) like lower(concat('%', :search, '%')) "
              + "or lower(p.content) like lower(concat('%', :search, '%')) "
              + "or lower(p.excerpt) like lower(concat('%', :search, '%')) "
              + "or lower(a.name) like lower(concat('%', :search, '%')) "
              + "or lower(t.name) like lower(concat('%', :search, '%')) "
              + "or lower(c.commentContent) like lower(concat('%', :search, '%'))"
              + ") "
              + "and (:authorNames is null or a.name in :authorNames) "
              + "and (:tagIds is null or t.id in :tagIds) "
              + "and (cast(:fromDate as timestamp) is null or p.publishedAt >= :fromDate) "
              + "and (cast(:toDate as timestamp) is null or p.publishedAt <= :toDate) "
              + "group by p.id "
              + "having (:tagIds is null or count(distinct t.id) = :tagCount)",
      countQuery =
          "select count(distinct p) "
              + "from Post p "
              + "join p.author a "
              + "left join p.tags t "
              + "left join p.comments c "
              + "where ("
              + "lower(p.title) like lower(concat('%', :search, '%')) "
              + "or lower(p.content) like lower(concat('%', :search, '%')) "
              + "or lower(p.excerpt) like lower(concat('%', :search, '%')) "
              + "or lower(a.name) like lower(concat('%', :search, '%')) "
              + "or lower(t.name) like lower(concat('%', :search, '%')) "
              + "or lower(c.commentContent) like lower(concat('%', :search, '%'))"
              + ") "
              + "and (:authorNames is null or a.name in :authorNames) "
              + "and (:tagIds is null or t.id in :tagIds) "
              + "and (cast(:fromDate as timestamp) is null or p.publishedAt >= :fromDate) "
              + "and (cast(:toDate as timestamp) is null or p.publishedAt <= :toDate) "
              + "group by p.id "
              + "having (:tagIds is null or count(distinct t.id) = :tagCount)")
  Page<Post> findAll(
      @Param("authorNames") List<String> authorNames,
      @Param("tagIds") List<Long> tagIds,
      @Param("search") String searchString,
      @Param("fromDate") Instant fromDate,
      @Param("toDate") Instant toDate,
      @Param("tagCount") long tagCount,
      Pageable pageable);

  @Query("select distinct p.author.name from Post p where p.author is not null")
  Set<String> findDistinctAuthors();

  Optional<Post> getPostById(Long id);
}
