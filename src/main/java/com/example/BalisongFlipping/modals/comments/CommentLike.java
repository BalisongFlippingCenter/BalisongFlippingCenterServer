package com.example.BalisongFlipping.modals.comments;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "comment_likes")
public class CommentLike {

    @EmbeddedId
    private CommentLikeId id;

    public CommentLike() {}

    public CommentLike(CommentLikeId id) { this.id = id; }

    public CommentLikeId getId() { return id; }
    public void setId(CommentLikeId id) { this.id = id; }
}
