package com.example.BalisongFlipping.modals.posts;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PostLikeId implements Serializable {

    private Long accountId;
    private Long postId;

    public PostLikeId() {}

    public PostLikeId(Long accountId, Long postId) {
        this.accountId = accountId;
        this.postId = postId;
    }

    public Long getAccountId() { return accountId; }
    public Long getPostId() { return postId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostLikeId)) return false;
        PostLikeId that = (PostLikeId) o;
        return Objects.equals(accountId, that.accountId) && Objects.equals(postId, that.postId);
    }

    @Override
    public int hashCode() { return Objects.hash(accountId, postId); }
}
