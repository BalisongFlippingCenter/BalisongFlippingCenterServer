package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.commentDtos.CommentAuthorDto;
import com.example.BalisongFlipping.dtos.commentDtos.CommentResponseDto;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.comments.Comment;
import com.example.BalisongFlipping.modals.comments.CommentLike;
import com.example.BalisongFlipping.modals.comments.CommentLikeId;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CommentLikeRepository;
import com.example.BalisongFlipping.repositories.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class CommentService {

    @Autowired private CommentRepository commentRepository;
    @Autowired private CommentLikeRepository commentLikeRepository;
    @Autowired private AccountRepository accountRepository;

    // -------------------------------------------------------------------------
    // Public reads
    // -------------------------------------------------------------------------

    public Page<CommentResponseDto> getComments(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creationDate"));
        return commentRepository
                .findByPostIdAndParentCommentIdIsNull(postId, pageable)
                .map(this::buildCommentResponse);
    }

    public Page<CommentResponseDto> getReplies(Long commentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "creationDate"));
        return commentRepository
                .findByParentCommentId(commentId, pageable)
                .map(this::buildCommentResponse);
    }

    // -------------------------------------------------------------------------
    // Auth-required writes
    // -------------------------------------------------------------------------

    @Transactional
    public CommentResponseDto createComment(Long postId, String accountId, String content, Long parentCommentId) throws Exception {
        if (content == null || content.isBlank())
            throw new Exception("Comment content cannot be empty.");

        if (parentCommentId != null) {
            Comment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new Exception("Parent comment not found."));
            if (!parent.getPostId().equals(postId))
                throw new Exception("Parent comment does not belong to this post.");
            parent.setReplyCount(parent.getReplyCount() + 1);
            commentRepository.save(parent);
        }

        Comment comment = new Comment(postId, Long.parseLong(accountId), content, parentCommentId);
        return buildCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponseDto editComment(Long commentId, String accountId, String content) throws Exception {
        if (content == null || content.isBlank())
            throw new Exception("Comment content cannot be empty.");

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new Exception("Comment not found."));

        if (!comment.getAccountId().equals(Long.parseLong(accountId)))
            throw new Exception("You can only edit your own comments.");

        comment.setContent(content);
        comment.setEditedDate(new Date());
        return buildCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId, String accountId) throws Exception {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new Exception("Comment not found."));

        if (!comment.getAccountId().equals(Long.parseLong(accountId)))
            throw new Exception("You can only delete your own comments.");

        // Decrement parent reply count if this is a reply
        if (comment.getParentCommentId() != null) {
            commentRepository.findById(comment.getParentCommentId()).ifPresent(parent -> {
                parent.setReplyCount(Math.max(0, parent.getReplyCount() - 1));
                commentRepository.save(parent);
            });
        }

        // Cascade to replies is handled by the DB ON DELETE CASCADE constraint
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public CommentResponseDto likeComment(Long commentId, String accountId) throws Exception {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new Exception("Comment not found."));

        CommentLikeId likeId = new CommentLikeId(Long.parseLong(accountId), commentId);
        if (commentLikeRepository.existsById(likeId))
            throw new Exception("Comment already liked.");

        commentLikeRepository.save(new CommentLike(likeId));
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);

        User user = (User) accountRepository.findById(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Account not found."));
        user.getLikedCommentIds().add(commentId);
        accountRepository.save(user);

        return buildCommentResponse(comment);
    }

    @Transactional
    public CommentResponseDto unlikeComment(Long commentId, String accountId) throws Exception {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new Exception("Comment not found."));

        CommentLikeId likeId = new CommentLikeId(Long.parseLong(accountId), commentId);
        if (!commentLikeRepository.existsById(likeId))
            throw new Exception("Comment not liked.");

        commentLikeRepository.deleteById(likeId);
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentRepository.save(comment);

        User user = (User) accountRepository.findById(Long.parseLong(accountId))
                .orElseThrow(() -> new Exception("Account not found."));
        user.getLikedCommentIds().remove(commentId);
        accountRepository.save(user);

        return buildCommentResponse(comment);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CommentResponseDto buildCommentResponse(Comment comment) {
        User author = accountRepository.findById(comment.getAccountId())
                .map(a -> (User) a)
                .orElseThrow(() -> new RuntimeException("Comment author not found."));

        CommentAuthorDto authorDto = new CommentAuthorDto(
                author.getId().toString(),
                author.getDisplayName(),
                author.getIdentifierCode(),
                author.getProfileImg()
        );

        return new CommentResponseDto(comment, authorDto);
    }
}
