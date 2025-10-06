package com.blogapp.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
//this table is mapping table as we don't have any relation in the post
//or the tag table.
public class PostTag extends BaseModel{
    @JoinColumn(name = "post_id")
    @ManyToOne
    private Post post;

    @JoinColumn(name = "tag_id")
    @ManyToOne
    private Tag tag;
}
