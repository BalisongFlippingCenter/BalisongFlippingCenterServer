package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.dtos.commentDtos.CommentAuthorDto;
import com.example.BalisongFlipping.dtos.commentDtos.CommentResponseDto;
import com.example.BalisongFlipping.dtos.commentDtos.CreateCommentDto;
import com.example.BalisongFlipping.dtos.commentDtos.EditCommentDto;
import com.example.BalisongFlipping.modals.comments.Comment;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.CommentService;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private UserDto selfDto() {
        return new UserDto("1", "flipper@example.com", true, "Flipper", "0001", "USER",
                null, null, null, null, null, null, false, null, null,
                null, null, null, null, null, null,
                Set.of(), Set.of(), Set.of(), 0, 0, 0);
    }

    private CommentResponseDto commentResponse(Long id, String content) {
        Comment comment = new Comment(5L, 1L, content, null);
        return new CommentResponseDto(comment, new CommentAuthorDto("1", "Flipper", "0001", null));
    }

    @Test
    void getCommentsReturnsPagedResults() throws Exception {
        when(commentService.getComments(eq(5L), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(commentResponse(1L, "Nice knife!"))));

        mockMvc.perform(get("/posts/any/{postId}/comments", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comment.content").value("Nice knife!"));
    }

    @Test
    void getCommentsReturnsNotFoundOnFailure() throws Exception {
        when(commentService.getComments(eq(999L), eq(0), eq(20)))
                .thenThrow(new RuntimeException("Post not found."));

        mockMvc.perform(get("/posts/any/{postId}/comments", 999))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Post not found."));
    }

    @Test
    void getRepliesReturnsPagedResults() throws Exception {
        when(commentService.getReplies(eq(1L), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(commentResponse(2L, "Agreed!"))));

        mockMvc.perform(get("/posts/any/{postId}/comments/{commentId}/replies", 5, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comment.content").value("Agreed!"));
    }

    @Test
    void createCommentSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(commentService.createComment(eq(5L), eq("1"), eq("Nice knife!"), eq(null)))
                .thenReturn(commentResponse(1L, "Nice knife!"));

        mockMvc.perform(post("/posts/{postId}/comments", 5)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateCommentDto("Nice knife!", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment.content").value("Nice knife!"));
    }

    @Test
    void createCommentRejectsServiceFailure() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(commentService.createComment(eq(999L), eq("1"), eq("Nice knife!"), eq(null)))
                .thenThrow(new Exception("Post not found."));

        mockMvc.perform(post("/posts/{postId}/comments", 999)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateCommentDto("Nice knife!", null))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Post not found."));
    }

    @Test
    void editCommentSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(commentService.editComment(eq(1L), eq("1"), eq("Updated")))
                .thenReturn(commentResponse(1L, "Updated"));

        mockMvc.perform(put("/posts/{postId}/comments/{commentId}", 5, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new EditCommentDto("Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment.content").value("Updated"));
    }

    @Test
    void editCommentRejectsWhenNotOwner() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(commentService.editComment(eq(1L), eq("1"), eq("Updated")))
                .thenThrow(new Exception("You can only edit your own comments."));

        mockMvc.perform(put("/posts/{postId}/comments/{commentId}", 5, 1)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new EditCommentDto("Updated"))))
                .andExpect(status().isConflict())
                .andExpect(content().string("You can only edit your own comments."));
    }

    @Test
    void deleteCommentSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(delete("/posts/{postId}/comments/{commentId}", 5, 1))
                .andExpect(status().isNoContent());
    }

    @Test
    void likeCommentSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(commentService.likeComment(eq(1L), eq("1"))).thenReturn(commentResponse(1L, "Nice knife!"));

        mockMvc.perform(post("/posts/{postId}/comments/{commentId}/like", 5, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment.content").value("Nice knife!"));
    }

    @Test
    void unlikeCommentSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(commentService.unlikeComment(eq(1L), eq("1"))).thenReturn(commentResponse(1L, "Nice knife!"));

        mockMvc.perform(delete("/posts/{postId}/comments/{commentId}/like", 5, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment.content").value("Nice knife!"));
    }
}
