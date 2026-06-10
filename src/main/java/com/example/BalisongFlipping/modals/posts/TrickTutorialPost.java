package com.example.BalisongFlipping.modals.posts;

import com.example.BalisongFlipping.enums.posts.tags.DifficultyTag;
import com.example.BalisongFlipping.enums.posts.tags.TechniqueTag;
import jakarta.persistence.*;

import java.util.List;

@Entity
@DiscriminatorValue("TRICK_TUTORIAL")
public class TrickTutorialPost extends PostWrapper {

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_tag")
    private DifficultyTag difficultyTag;

    // Max 2 technique tags enforced in PostService
    @ElementCollection
    @CollectionTable(name = "post_technique_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    @Enumerated(EnumType.STRING)
    private List<TechniqueTag> techniqueTags;

    public TrickTutorialPost() {
        super();
    }

    public DifficultyTag getDifficultyTag() { return difficultyTag; }
    public void setDifficultyTag(DifficultyTag difficultyTag) { this.difficultyTag = difficultyTag; }

    public List<TechniqueTag> getTechniqueTags() { return techniqueTags; }
    public void setTechniqueTags(List<TechniqueTag> techniqueTags) { this.techniqueTags = techniqueTags; }
}
