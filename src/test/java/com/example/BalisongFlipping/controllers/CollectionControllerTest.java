package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.CollectionDataDto;
import com.example.BalisongFlipping.dtos.KnifeGalleryUploadUrlRequestDto;
import com.example.BalisongFlipping.dtos.PublicProfileDto;
import com.example.BalisongFlipping.dtos.UpdateKnifeDto;
import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.dtos.uploadsDtos.FileUploadRequestItem;
import com.example.BalisongFlipping.modals.collectionKnives.CollectionKnife;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.CollectionService;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.PostService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.example.BalisongFlipping.services.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CollectionService collectionService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private PostService postService;

    @MockBean
    private S3Service s3Service;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private UserDto selfDto() {
        return new UserDto("1", "flipper@example.com", true, "Flipper", "0001", "USER",
                "5", null, null, null, null, null, false, null, null,
                null, null, null, null, null, null,
                Set.of(), Set.of(), Set.of(), 0, 0, 0);
    }

    private CollectionDataDto collectionData() {
        return new CollectionDataDto("5", "1", null, null, List.of());
    }

    private PublicProfileDto publicProfile() {
        return new PublicProfileDto("1", "Flipper", "0001", null, null, null, "5",
                false, null, null, null, null, null, null, null, null, 0, 0, 0);
    }

    private CollectionKnife collectionKnife(String displayName) {
        CollectionKnife knife = new CollectionKnife();
        knife.setDisplayName(displayName);
        return knife;
    }

    @Test
    void getCollectionByAccountIdSucceeds() throws Exception {
        when(accountService.getPublicProfileById("1")).thenReturn(publicProfile());
        when(collectionService.getCollectionByAccountId("1")).thenReturn(collectionData());

        mockMvc.perform(get("/collection/any/account/{accountId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Flipper"));
    }

    @Test
    void getCollectionByAccountIdReturnsNotFoundWhenNoCollection() throws Exception {
        when(accountService.getPublicProfileById("1")).thenReturn(publicProfile());
        when(collectionService.getCollectionByAccountId("1")).thenReturn(null);

        mockMvc.perform(get("/collection/any/account/{accountId}", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Collection not found."));
    }

    @Test
    void getCollectionByHandleSucceeds() throws Exception {
        when(accountService.getPublicProfileByHandle("Flipper", "0001")).thenReturn(publicProfile());
        when(collectionService.getCollectionByAccountId("1")).thenReturn(collectionData());

        mockMvc.perform(get("/collection/any/handle")
                        .param("displayName", "Flipper")
                        .param("identifierCode", "0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Flipper"));
    }

    @Test
    void getKnifeByIdSucceeds() throws Exception {
        when(collectionService.getKnifeById(7L)).thenReturn(collectionKnife("Mako"));

        mockMvc.perform(get("/collection/any/knife/{knifeId}", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Mako"));
    }

    @Test
    void getKnifeByIdReturnsNotFoundForUnknownKnife() throws Exception {
        when(collectionService.getKnifeById(999L)).thenReturn(null);

        mockMvc.perform(get("/collection/any/knife/{knifeId}", 999))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Knife not found."));
    }

    @Test
    void getCollectionPostsSucceeds() throws Exception {
        when(collectionService.getCollection("5")).thenReturn(collectionData());
        when(postService.getCollectionTimelinePosts("1")).thenReturn(List.of());

        mockMvc.perform(get("/collection/any/{collectionId}/get-posts", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void getCollectionByIdSucceeds() throws Exception {
        when(collectionService.getCollection("5")).thenReturn(collectionData());

        mockMvc.perform(get("/collection/any/{collectionId}", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("5"));
    }

    @Test
    void getCollectionByIdReturnsNotFoundForUnknownId() throws Exception {
        when(collectionService.getCollection("999")).thenReturn(null);

        mockMvc.perform(get("/collection/any/{collectionId}", "999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Error retrieving collection"));
    }

    @Test
    void updateBannerImgSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(collectionService.checkForCollectionExistance("5")).thenReturn(true);
        when(collectionService.updateBannerImg(eq("5"), anyString())).thenReturn(collectionData());

        MockMultipartFile file = new MockMultipartFile("file", "banner.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/collection/me/update-banner-img").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void updateBannerImgRejectsWhenCollectionMissing() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(collectionService.checkForCollectionExistance("5")).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile("file", "banner.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/collection/me/update-banner-img").file(file))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Collection doesn't exist"));
    }

    @Test
    void setFeaturedKnifeSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(collectionService.setFeaturedKnife("5", "7")).thenReturn(collectionData());

        mockMvc.perform(post("/collection/me/set-featured-knife/{knifeId}", "7"))
                .andExpect(status().isOk());
    }

    @Test
    void clearFeaturedKnifeSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(collectionService.clearFeaturedKnife("5")).thenReturn(collectionData());

        mockMvc.perform(post("/collection/me/clear-featured-knife"))
                .andExpect(status().isOk());
    }

    @Test
    void updateKnifeCoverPhotoSucceedsWithExistingUrl() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(collectionService.updateKnifeCoverPhoto("5", "7", "https://existing/photo.png"))
                .thenReturn(collectionKnife("Mako"));

        mockMvc.perform(multipart("/collection/me/update-knife/{knifeId}/cover-photo", "7")
                        .param("existingUrl", "https://existing/photo.png"))
                .andExpect(status().isOk());
    }

    @Test
    void updateKnifeCoverPhotoRejectsWhenNeitherProvided() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(multipart("/collection/me/update-knife/{knifeId}/cover-photo", "7"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Either a file or an existing URL must be provided."));
    }

    @Test
    void removeKnifeSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(delete("/collection/me/remove-knife/{knifeId}", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string("Knife removed from collection."));
    }

    @Test
    void updateKnifeSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(collectionService.updateKnife(eq("5"), eq("7"), any())).thenReturn(collectionKnife("Mako Updated"));

        UpdateKnifeDto dto = new UpdateKnifeDto("Mako Updated", "Squid Industries", "Mako", "trainer",
                "2024-01-01", true, false, 150.0, 5.5, 4.0, "bushing", "over-travel", "captive",
                false, null, "reverse-tanto", "stonewash", "s35vn", "milled", "g10", "stonewash",
                5, 5, 5, 5, 5);

        mockMvc.perform(put("/collection/me/update-knife/{knifeId}", "7")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Mako Updated"));
    }

    @Test
    void getKnifeGalleryUploadUrlsRejectsMissingDisplayName() throws Exception {
        mockMvc.perform(post("/collection/me/knife-gallery-upload-url")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new KnifeGalleryUploadUrlRequestDto(null, List.of()))))
                .andExpect(status().isConflict())
                .andExpect(content().string("displayName is required."));
    }

    @Test
    void getKnifeGalleryUploadUrlsRejectsTooManyFiles() throws Exception {
        List<FileUploadRequestItem> files = List.of(
                new FileUploadRequestItem("1.png", "image/png"), new FileUploadRequestItem("2.png", "image/png"),
                new FileUploadRequestItem("3.png", "image/png"), new FileUploadRequestItem("4.png", "image/png"),
                new FileUploadRequestItem("5.png", "image/png"), new FileUploadRequestItem("6.png", "image/png"),
                new FileUploadRequestItem("7.png", "image/png"), new FileUploadRequestItem("8.png", "image/png"),
                new FileUploadRequestItem("9.png", "image/png"), new FileUploadRequestItem("10.png", "image/png"),
                new FileUploadRequestItem("11.png", "image/png"));

        mockMvc.perform(post("/collection/me/knife-gallery-upload-url")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new KnifeGalleryUploadUrlRequestDto("Mako", files))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Gallery cannot exceed 10 files."));
    }

    @Test
    void addKnifeToCollectionSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(collectionService.checkForCollectionExistance("5")).thenReturn(true);
        when(collectionService.validateNewKnifeInfo(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);
        when(collectionService.checkForDuplicateDisplayName(eq("Mako"), eq("5"))).thenReturn(false);
        when(collectionService.addNewKnife(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(collectionKnife("Mako"));

        MockMultipartFile coverPhoto = new MockMultipartFile("coverPhoto", "mako.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/collection/me/add-knife")
                        .file(coverPhoto)
                        .param("displayName", "Mako")
                        .param("knifeMaker", "Squid Industries")
                        .param("baseKnifeModel", "Mako")
                        .param("knifeType", "trainer")
                        .param("aqquiredDate", "2024-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Mako"));
    }

    @Test
    void addKnifeToCollectionRejectsDuplicateDisplayName() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(collectionService.checkForCollectionExistance("5")).thenReturn(true);
        when(collectionService.validateNewKnifeInfo(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);
        when(collectionService.checkForDuplicateDisplayName(eq("Mako"), eq("5"))).thenReturn(true);

        MockMultipartFile coverPhoto = new MockMultipartFile("coverPhoto", "mako.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/collection/me/add-knife")
                        .file(coverPhoto)
                        .param("displayName", "Mako")
                        .param("knifeMaker", "Squid Industries")
                        .param("baseKnifeModel", "Mako")
                        .param("knifeType", "trainer")
                        .param("aqquiredDate", "2024-01-01"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Duplicate display name."));
    }
}
