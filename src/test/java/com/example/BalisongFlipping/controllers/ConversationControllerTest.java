package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.dtos.UserDto;
import com.example.BalisongFlipping.dtos.messagingDtos.ConversationDto;
import com.example.BalisongFlipping.dtos.messagingDtos.MessageDto;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.ConversationService;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import com.example.BalisongFlipping.services.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private S3Service s3Service;

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

    private MessageDto messageDto(Long id, String body) {
        return new MessageDto(id, 1L, "1", body, null, false, null, null, null, null, null, false, null);
    }

    @Test
    void getInboxReturnsConversations() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(conversationService.getInbox("1")).thenReturn(List.of(
                new ConversationDto(1L, "2", "OtherFlipper", "0002", null, "Hey!", null, 1)));

        mockMvc.perform(get("/conversations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].otherDisplayName").value("OtherFlipper"));
    }

    @Test
    void getInboxRejectsServiceFailure() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(conversationService.getInbox("1")).thenThrow(new RuntimeException("Lookup failed"));

        mockMvc.perform(get("/conversations/me"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Lookup failed"));
    }

    @Test
    void getMessagesReturnsPagedResults() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(conversationService.getMessages(eq(1L), eq("1"), eq(0), eq(30)))
                .thenReturn(new PageImpl<>(List.of(messageDto(10L, "Hey!"))));

        mockMvc.perform(get("/conversations/{id}/messages", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].body").value("Hey!"));
    }

    @Test
    void sendMessageSucceedsWithTextOnly() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(conversationService.sendMessage(eq("1"), eq("2"), eq("Hey!"), isNull(), eq(false), isNull()))
                .thenReturn(messageDto(10L, "Hey!"));

        mockMvc.perform(multipart("/conversations/{recipientId}/messages", "2")
                        .param("body", "Hey!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Hey!"));
    }

    @Test
    void sendMessageSucceedsWithImage() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        MockMultipartFile image = new MockMultipartFile("mediaFile", "knife.png", "image/png", new byte[]{1, 2, 3});
        when(conversationService.sendMessage(eq("1"), eq("2"), isNull(), any(), eq(false), isNull()))
                .thenReturn(messageDto(11L, null));

        mockMvc.perform(multipart("/conversations/{recipientId}/messages", "2").file(image))
                .andExpect(status().isCreated());
    }

    @Test
    void sendMessageRejectsDisallowedFileType() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        MockMultipartFile file = new MockMultipartFile("mediaFile", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/conversations/{recipientId}/messages", "2").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Only image and video files are allowed."));
    }

    @Test
    void sendMessageRejectsOversizedImage() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        byte[] tooLarge = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("mediaFile", "big.png", "image/png", tooLarge);

        mockMvc.perform(multipart("/conversations/{recipientId}/messages", "2").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("File exceeds the 10 MB limit."));
    }

    @Test
    void editMessageSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(conversationService.editMessage(eq("1"), eq(10L), eq("Updated")))
                .thenReturn(messageDto(10L, "Updated"));

        mockMvc.perform(patch("/conversations/messages/{msgId}", 10)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("body", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Updated"));
    }

    @Test
    void deleteMessageSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());
        when(conversationService.deleteMessage(eq("1"), eq(10L))).thenReturn(messageDto(10L, null));

        mockMvc.perform(delete("/conversations/messages/{msgId}", 10))
                .andExpect(status().isOk());
    }

    @Test
    void markReadSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(patch("/conversations/{id}/read", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Conversation marked as read."));
    }

    @Test
    void deleteConversationSucceeds() throws Exception {
        when(accountService.getSelf()).thenReturn(selfDto());

        mockMvc.perform(delete("/conversations/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Conversation deleted."));
    }
}
