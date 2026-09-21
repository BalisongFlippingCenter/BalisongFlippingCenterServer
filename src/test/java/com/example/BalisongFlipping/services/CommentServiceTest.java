package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.commentDtos.CommentResponseDto;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.comments.Comment;
import com.example.BalisongFlipping.modals.posts.GenericPost;
import com.example.BalisongFlipping.modals.posts.PostWrapper;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CommentLikeRepository;
import com.example.BalisongFlipping.repositories.CommentRepository;
import com.example.BalisongFlipping.repositories.PostsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PostsRepository postsRepository;
    @Mock private NotificationService notificationService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService();
        ReflectionTestUtils.setField(commentService, "commentRepository", commentRepository);
        ReflectionTestUtils.setField(commentService, "commentLikeRepository", commentLikeRepository);
        ReflectionTestUtils.setField(commentService, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(commentService, "postsRepository", postsRepository);
        ReflectionTestUtils.setField(commentService, "notificationService", notificationService);
    }

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setDisplayName("Flipper" + id);
        return u;
    }

    private GenericPost post(Long id, String accountId) {
        GenericPost p = new GenericPost();
        p.setId(id);
        p.setAccountId(accountId);
        return p;
    }

    private Comment comment(Long id, Long postId, Long accountId, Long parentCommentId) {
        Comment c = new Comment(postId, accountId, "Nice knife!", parentCommentId);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    // -------------------------------------------------------------------------
    // getComments / getReplies
    // -------------------------------------------------------------------------

    @Test
    void getCommentsReturnsTopLevelComments() {
        when(commentRepository.findByPostIdAndParentCommentIdIsNull(eq(5L), any()))
                .thenReturn(new PageImpl<>(List.of(comment(1L, 5L, 1L, null))));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        var result = commentService.getComments(5L, 0, 20);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getRepliesReturnsRepliesForParent() {
        when(commentRepository.findByParentCommentId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(comment(2L, 5L, 1L, 1L))));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        var result = commentService.getReplies(1L, 0, 20);

        assertEquals(1, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // createComment
    // -------------------------------------------------------------------------

    @Test
    void createCommentSucceedsAndNotifiesPostAuthor() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        GenericPost p = post(5L, "2");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(p));
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        CommentResponseDto result = commentService.createComment(5L, "1", "Nice knife!", null);

        assertEquals("Nice knife!", result.comment().getContent());
        assertEquals(1, p.getCommentCount());
        verify(notificationService).send(eq(2L), eq(1L), any(), any(), eq(5L));
    }

    @Test
    void createCommentRejectsEmptyContent() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        Exception ex = assertThrows(Exception.class, () -> commentService.createComment(5L, "1", " ", null));
        assertEquals("Comment content cannot be empty.", ex.getMessage());
    }

    @Test
    void createCommentRejectsMutedAccount() {
        User muted = user(1L);
        muted.setMutedUntil(java.time.Instant.now().plusSeconds(3600));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(muted));

        Exception ex = assertThrows(Exception.class, () -> commentService.createComment(5L, "1", "Nice!", null));
        assertTrue(ex.getMessage().startsWith("You are muted until"));
    }

    @Test
    void createCommentAsReplyIncrementsParentReplyCountAndNotifiesParentAuthor() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        GenericPost p = post(5L, "2");
        when(postsRepository.findById(5L)).thenReturn(Optional.of(p));
        Comment parent = comment(10L, 5L, 3L, null);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        commentService.createComment(5L, "1", "Reply!", 10L);

        assertEquals(1, parent.getReplyCount());
        verify(notificationService).send(eq(3L), eq(1L), any(), any(), eq(5L));
        verify(notificationService, never()).send(eq(2L), any(), any(), any(), any());
    }

    @Test
    void createCommentRejectsParentFromDifferentPost() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(postsRepository.findById(5L)).thenReturn(Optional.of(post(5L, "2")));
        Comment parent = comment(10L, 999L, 3L, null);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));

        Exception ex = assertThrows(Exception.class, () -> commentService.createComment(5L, "1", "Reply!", 10L));
        assertEquals("Parent comment does not belong to this post.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // editComment
    // -------------------------------------------------------------------------

    @Test
    void editCommentSucceeds() throws Exception {
        Comment c = comment(1L, 5L, 1L, null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentRepository.save(c)).thenReturn(c);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        CommentResponseDto result = commentService.editComment(1L, "1", "Updated");

        assertEquals("Updated", result.comment().getContent());
    }

    @Test
    void editCommentRejectsNonOwner() {
        Comment c = comment(1L, 5L, 1L, null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

        Exception ex = assertThrows(Exception.class, () -> commentService.editComment(1L, "2", "Updated"));
        assertEquals("You can only edit your own comments.", ex.getMessage());
    }

    @Test
    void editCommentRejectsEmptyContent() {
        Exception ex = assertThrows(Exception.class, () -> commentService.editComment(1L, "1", ""));
        assertEquals("Comment content cannot be empty.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // deleteComment
    // -------------------------------------------------------------------------

    @Test
    void deleteTopLevelCommentDecrementsPostCommentCountByItselfAndReplies() throws Exception {
        Comment c = comment(1L, 5L, 1L, null);
        c.setReplyCount(2);
        GenericPost p = post(5L, "1");
        p.setCommentCount(5);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(postsRepository.findById(5L)).thenReturn(Optional.of(p));

        commentService.deleteComment(1L, "1");

        assertEquals(2, p.getCommentCount());
        verify(commentRepository).deleteById(1L);
    }

    @Test
    void deleteReplyDecrementsParentReplyCountAndPostCommentCount() throws Exception {
        Comment reply = comment(2L, 5L, 1L, 10L);
        Comment parent = comment(10L, 5L, 3L, null);
        parent.setReplyCount(1);
        GenericPost p = post(5L, "1");
        p.setCommentCount(3);
        when(commentRepository.findById(2L)).thenReturn(Optional.of(reply));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(postsRepository.findById(5L)).thenReturn(Optional.of(p));

        commentService.deleteComment(2L, "1");

        assertEquals(0, parent.getReplyCount());
        assertEquals(2, p.getCommentCount());
    }

    @Test
    void deleteCommentRejectsNonOwner() {
        Comment c = comment(1L, 5L, 1L, null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

        Exception ex = assertThrows(Exception.class, () -> commentService.deleteComment(1L, "2"));
        assertEquals("You can only delete your own comments.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // likeComment / unlikeComment
    // -------------------------------------------------------------------------

    @Test
    void likeCommentSucceeds() throws Exception {
        Comment c = comment(1L, 5L, 2L, null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentLikeRepository.existsById(any())).thenReturn(false);
        User liker = user(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(liker));

        CommentResponseDto result = commentService.likeComment(1L, "1");

        assertEquals(1, c.getLikeCount());
        assertTrue(liker.getLikedCommentIds().contains(1L));
        verify(notificationService).send(eq(2L), eq(1L), any(), any(), eq(5L));
    }

    @Test
    void likeCommentRejectsDuplicateLike() {
        Comment c = comment(1L, 5L, 2L, null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentLikeRepository.existsById(any())).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> commentService.likeComment(1L, "1"));
        assertEquals("Comment already liked.", ex.getMessage());
    }

    @Test
    void unlikeCommentSucceeds() throws Exception {
        Comment c = comment(1L, 5L, 2L, null);
        c.setLikeCount(1);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentLikeRepository.existsById(any())).thenReturn(true);
        User liker = user(1L);
        liker.getLikedCommentIds().add(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(liker));

        commentService.unlikeComment(1L, "1");

        assertEquals(0, c.getLikeCount());
        assertTrue(liker.getLikedCommentIds().isEmpty());
    }

    @Test
    void unlikeCommentRejectsWhenNotLiked() {
        Comment c = comment(1L, 5L, 2L, null);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentLikeRepository.existsById(any())).thenReturn(false);

        Exception ex = assertThrows(Exception.class, () -> commentService.unlikeComment(1L, "1"));
        assertEquals("Comment not liked.", ex.getMessage());
    }
}
