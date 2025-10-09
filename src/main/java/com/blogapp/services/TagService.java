package com.blogapp.services;

import com.blogapp.models.Tag;
import com.blogapp.repositories.TagRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public String[] extractTags(String tagListString){
        // Logic to extract tags from the comma-separated string
        String[] tagNames = tagListString.split(",");
        for(int i = 0; i < tagNames.length; i++){
            tagNames[i] = tagNames[i].trim();
        }
        return tagNames;
    }

    public List<Tag> saveTags(String tagListString){
        String[] tagNamesArray = extractTags(tagListString);
        List<Tag> savedTags = new ArrayList<>();

        for(String tagName : tagNamesArray){
            Tag tag = tagRepository.findByName(tagName);
            if(tag == null){
                tag = new Tag();
                tag.setName(tagName);
                savedTags.add(tagRepository.save(tag));
            }
        }
        return savedTags;
    }
}
