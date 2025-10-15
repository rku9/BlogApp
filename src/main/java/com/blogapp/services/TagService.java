package com.blogapp.services;

import com.blogapp.models.Tag;
import com.blogapp.repositories.TagRepository;

import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

@Service
public class TagService {

  private final TagRepository tagRepository;

  public TagService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  /**
   * Extracts tag names from a comma-separated list.
   *
   * @param tagListString raw comma-separated tag names
   * @return an array of trimmed, lowercased tag names
   */
  public String[] extractTags(String tagListString) {
    String[] tagNames = tagListString.split(",");
    for (int i = 0; i < tagNames.length; i++) {
      tagNames[i] = tagNames[i].trim().toLowerCase();
    }
    return tagNames;
  }

  /**
   * Persists tags derived from the comma-separated string if they do not already exist.
   *
   * @param tagListString raw comma-separated tag names
   * @return a set containing saved tag entities
   */
  public Set<Tag> saveTags(String tagListString) {
    String[] tagNamesArray = extractTags(tagListString);
    Set<Tag> savedTags = new HashSet<>();

    for (String tagName : tagNamesArray) {
      Tag tag = tagRepository.findByName(tagName);
      if (tag == null) {
        tag = new Tag();
        tag.setName(tagName);
        savedTags.add(tagRepository.save(tag));
      } else {
        savedTags.add(tag);
      }
    }
    return savedTags;
  }

  /**
   * Retrieves all non-deleted tags sorted alphabetically.
   *
   * @return a sorted set of active tags
   */
  public Set<Tag> findAllTags() {
    return new TreeSet<>(tagRepository.findAllByDeletedFalse());
  }

  @Transactional
  public void deleteUnusedTags(Set<Tag> tags) {
    for (Tag tag : tags) {
      long count = tagRepository.countPostsByTagIdAndIsDeletedFalse(tag.getId());
      if (count == 0) {
        tag.setDeleted(true);
      }
    }
  }
}
