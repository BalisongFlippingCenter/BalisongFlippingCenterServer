package com.example.BalisongFlipping.modals.posts;

import com.example.BalisongFlipping.enums.posts.tags.GenericPostTag;
import jakarta.persistence.*;

import java.util.List;

@Entity
@DiscriminatorValue("GENERIC")
public class GenericPost extends PostWrapper {

    @ElementCollection
    @CollectionTable(name = "post_generic_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    @Enumerated(EnumType.STRING)
    private List<GenericPostTag> tags;

    public GenericPost() {
        super();
    }

    public List<GenericPostTag> getTags() { return tags; }
    public void setTags(List<GenericPostTag> tags) { this.tags = tags; }
}
