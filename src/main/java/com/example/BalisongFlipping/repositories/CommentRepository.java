package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.comments.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdAndParentCommentIdIsNull(Long postId, Pageable pageable);

    Page<Comment> findByParentCommentId(Long parentCommentId, Pageable pageable);

    void deleteAllByPostId(Long postId);

    void deleteAllByAccountId(Long accountId);
}
