package com.example.BalisongFlipping.modals.follows;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "follows")
public class Follow {

    @EmbeddedId
    private FollowId id;

    public Follow() {}

    public Follow(FollowId id) { this.id = id; }

    public FollowId getId() { return id; }
    public void setId(FollowId id) { this.id = id; }
}
