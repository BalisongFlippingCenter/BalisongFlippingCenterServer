package com.example.BalisongFlipping.modals.comments;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CommentLikeId implements Serializable {

    private Long accountId;
    private Long commentId;

    public CommentLikeId() {}

    public CommentLikeId(Long accountId, Long commentId) {
        this.accountId = accountId;
        this.commentId = commentId;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentLikeId that)) return false;
        return Objects.equals(accountId, that.accountId) && Objects.equals(commentId, that.commentId);
    }

    @Override
    public int hashCode() { return Objects.hash(accountId, commentId); }
}
