package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.dtos.postsDtos.CreatePostRequestDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostResponseDto;
import com.example.BalisongFlipping.dtos.postsDtos.PostUploadUrlRequestDto;
import com.example.BalisongFlipping.dtos.postsDtos.UpdatePostDto;
import com.example.BalisongFlipping.dtos.uploadsDtos.FileUploadRequestItem;
import com.example.BalisongFlipping.dtos.uploadsDtos.PresignedUploadTargetDto;
import com.example.BalisongFlipping.modals.posts.GenericPost;
import com.example.BalisongFlipping.modals.posts.PostWrapper;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.PostService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

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

    private PostWrapper genericPost(Long id, String caption) {
        GenericPost post = new GenericPost();
        post.setId(id);
        post.setAccountId("1");
        post.setCaption(caption);
        return post;
    }

    private PostResponseDto postResponse(Long id, String caption) {
        return new PostResponseDto(genericPost(id, caption), null, null, null);
    }

    @Test
    void getPostByIdReturnsPost() throws Exception {
        when(postService.getPostById(5L)).thenReturn(postResponse(5L, "Nice flip"));

        mockMvc.perform(get("/posts/any/{id}", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.post.caption").value("Nice flip"));
    }

    @Test
    void getPostByIdReturnsNotFoundForUnknownId() throws Exception {
        when(postService.getPostById(999L)).thenThrow(new RuntimeException("Post not found."));

        mockMvc.perform(get("/posts/any/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Post not found."));
    }

    @Test
    void getPostsReturnsPagedResults() throws Exception {
        when(accountService.getSelf()).thenThrow(new RuntimeException("no auth"));
        when(postService.getPosts(any(), any(), any(), any(), anyInt(), anyInt(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(postResponse(5L, "Nice flip"))));

        mockMvc.perform(get("/posts/any"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].post.caption").value("Nice flip"));
    }

    @Test
    void getUploadUrlsSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(postService.generateUploadUrls(eq("1"), any()))
                .thenReturn(List.of(new PresignedUploadTargetDto("posts/1/a.png", "https://upload", "https://public", false)));

        mockMvc.perform(post("/posts/upload-url")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new PostUploadUrlRequestDto("GENERIC", List.of(new FileUploadRequestItem("a.png", "image/png"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("posts/1/a.png"));
    }

    @Test
    void createPostSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(postService.createPost(eq("1"), any())).thenReturn(genericPost(10L, "New post"));

        mockMvc.perform(post("/posts/create")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreatePostRequestDto(
                                "GENERIC", "New post", null, null, List.of(), null, null, null, null, List.of(), null, List.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caption").value("New post"));
    }

    @Test
    void createPostRejectsServiceFailure() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(postService.createPost(eq("1"), any())).thenThrow(new Exception("Invalid post type."));

        mockMvc.perform(post("/posts/create")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreatePostRequestDto(
                                "BOGUS", "New post", null, null, List.of(), null, null, null, null, List.of(), null, List.of()))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Invalid post type."));
    }

    @Test
    void getLikedPostsReturnsPagedResults() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(postService.getLikedPosts(eq("1"), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(postResponse(5L, "Nice flip"))));

        mockMvc.perform(get("/posts/me/liked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].post.caption").value("Nice flip"));
    }

    @Test
    void updatePostSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(postService.updatePost(eq(5L), eq("1"), any())).thenReturn(postResponse(5L, "Updated caption"));

        mockMvc.perform(patch("/posts/{id}", 5)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdatePostDto("Updated caption", null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.post.caption").value("Updated caption"));
    }

    @Test
    void updatePostRejectsWhenNotOwner() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(postService.updatePost(eq(5L), eq("1"), any())).thenThrow(new Exception("You can only edit your own posts."));

        mockMvc.perform(patch("/posts/{id}", 5)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdatePostDto("Updated caption", null, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(content().string("You can only edit your own posts."));
    }

    @Test
    void deletePostSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(delete("/posts/{id}", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post deleted."));
    }

    @Test
    void likePostSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(postService.likePost(eq(5L), eq("1"))).thenReturn(postResponse(5L, "Nice flip"));

        mockMvc.perform(post("/posts/{id}/like", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.post.caption").value("Nice flip"));
    }

    @Test
    void unlikePostSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(postService.unlikePost(eq(5L), eq("1"))).thenReturn(postResponse(5L, "Nice flip"));

        mockMvc.perform(delete("/posts/{id}/like", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.post.caption").value("Nice flip"));
    }
}
