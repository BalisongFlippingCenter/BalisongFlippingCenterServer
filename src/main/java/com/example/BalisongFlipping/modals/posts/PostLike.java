package com.example.BalisongFlipping.modals.posts;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_likes")
public class PostLike {

    @EmbeddedId
    private PostLikeId id;

    public PostLike() {}

    public PostLike(PostLikeId id) { this.id = id; }

    public PostLikeId getId() { return id; }
    public void setId(PostLikeId id) { this.id = id; }
}
