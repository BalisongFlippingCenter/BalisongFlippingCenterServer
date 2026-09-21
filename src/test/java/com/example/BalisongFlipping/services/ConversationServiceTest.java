package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.messagingDtos.ConversationDto;
import com.example.BalisongFlipping.dtos.messagingDtos.MessageDto;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.messaging.Conversation;
import com.example.BalisongFlipping.modals.messaging.Message;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.ConversationRepository;
import com.example.BalisongFlipping.repositories.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService();
        ReflectionTestUtils.setField(conversationService, "conversationRepository", conversationRepository);
        ReflectionTestUtils.setField(conversationService, "messageRepository", messageRepository);
        ReflectionTestUtils.setField(conversationService, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(conversationService, "notificationService", notificationService);
        ReflectionTestUtils.setField(conversationService, "emailService", emailService);
        ReflectionTestUtils.setField(conversationService, "messagingTemplate", messagingTemplate);
    }

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("flipper" + id + "@example.com");
        u.setDisplayName("Flipper" + id);
        return u;
    }

    private Conversation conversation(Long id, Long a, Long b) {
        Conversation c = new Conversation();
        ReflectionTestUtils.setField(c, "id", id);
        c.setParticipantAId(a);
        c.setParticipantBId(b);
        return c;
    }

    private Message message(Long id, Long conversationId, Long senderId) {
        Message m = new Message();
        ReflectionTestUtils.setField(m, "id", id);
        m.setConversationId(conversationId);
        m.setSenderId(senderId);
        return m;
    }

    // -------------------------------------------------------------------------
    // getInbox
    // -------------------------------------------------------------------------

    @Test
    void getInboxMapsConversationsForRequester() throws Exception {
        Conversation c = conversation(1L, 1L, 2L);
        c.setLastMessagePreview("Hey!");
        c.setUnreadCountA(3);
        when(conversationRepository.findInboxForUser(1L)).thenReturn(List.of(c));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

        List<ConversationDto> inbox = conversationService.getInbox("1");

        assertEquals(1, inbox.size());
        assertEquals("2", inbox.get(0).otherParticipantId());
        assertEquals(3, inbox.get(0).unreadCount());
    }

    // -------------------------------------------------------------------------
    // getMessages
    // -------------------------------------------------------------------------

    @Test
    void getMessagesReturnsPageForParticipant() throws Exception {
        Conversation c = conversation(1L, 1L, 2L);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(c));
        when(messageRepository.findByConversationId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(message(10L, 1L, 1L))));

        var page = conversationService.getMessages(1L, "1", 0, 30);

        assertEquals(1, page.getContent().size());
    }

    @Test
    void getMessagesRejectsNonParticipant() {
        Conversation c = conversation(1L, 1L, 2L);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(c));

        Exception ex = assertThrows(Exception.class, () -> conversationService.getMessages(1L, "999", 0, 30));
        assertEquals("You are not a participant in this conversation.", ex.getMessage());
    }

    @Test
    void getMessagesThrowsForUnknownConversation() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> conversationService.getMessages(999L, "1", 0, 30));
    }

    // -------------------------------------------------------------------------
    // sendMessage
    // -------------------------------------------------------------------------

    @Test
    void sendMessageRejectsEmptyMessage() {
        Exception ex = assertThrows(Exception.class, () -> conversationService.sendMessage("1", "2", null, null, false, null));
        assertEquals("A message must contain text or a media file.", ex.getMessage());
    }

    @Test
    void sendMessageRejectsTooLongBody() {
        String longBody = "a".repeat(2001);
        Exception ex = assertThrows(Exception.class, () -> conversationService.sendMessage("1", "2", longBody, null, false, null));
        assertEquals("Message body may not exceed 2000 characters.", ex.getMessage());
    }

    @Test
    void sendMessageRejectsSelfMessage() {
        Exception ex = assertThrows(Exception.class, () -> conversationService.sendMessage("1", "1", "hey", null, false, null));
        assertEquals("Cannot message yourself.", ex.getMessage());
    }

    @Test
    void sendMessageRejectsUnknownRecipient() {
        when(accountRepository.findById(2L)).thenReturn(Optional.empty());
        Exception ex = assertThrows(Exception.class, () -> conversationService.sendMessage("1", "2", "hey", null, false, null));
        assertEquals("Recipient not found.", ex.getMessage());
    }

    @Test
    void sendMessageCreatesNewConversationAndSendsEmail() throws Exception {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(conversationRepository.findByParticipantAIdAndParticipantBId(1L, 2L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 5L);
            return c;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 10L);
            return m;
        });

        MessageDto result = conversationService.sendMessage("1", "2", "Hey!", null, false, null);

        assertEquals("Hey!", result.body());
        verify(emailService).sendEmail(eq("flipper2@example.com"), anyString(), anyString());
        verify(notificationService).send(eq(2L), eq(1L), any(), any(), eq(5L));
        verify(messagingTemplate).convertAndSendToUser(eq("flipper2@example.com"), eq("/queue/messages"), any());
    }

    @Test
    void sendMessageToActiveConversationSkipsEmail() throws Exception {
        Conversation existing = conversation(5L, 1L, 2L);
        existing.setLastMessageAt(new Date());
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(conversationRepository.findByParticipantAIdAndParticipantBId(1L, 2L)).thenReturn(Optional.of(existing));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(existing);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 10L);
            return m;
        });

        conversationService.sendMessage("1", "2", "Hey again!", null, false, null);

        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    void sendMessageRejectsReplyFromDifferentConversation() throws Exception {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(conversationRepository.findByParticipantAIdAndParticipantBId(1L, 2L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 5L);
            return c;
        });
        Message otherConversationMessage = message(99L, 999L, 2L);
        when(messageRepository.findById(99L)).thenReturn(Optional.of(otherConversationMessage));

        Exception ex = assertThrows(Exception.class, () -> conversationService.sendMessage("1", "2", "Hey!", null, false, 99L));
        assertEquals("Cannot reply to a message from a different conversation.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // editMessage
    // -------------------------------------------------------------------------

    @Test
    void editMessageSucceeds() throws Exception {
        Message m = message(10L, 1L, 1L);
        m.setBody("Old");
        when(messageRepository.findById(10L)).thenReturn(Optional.of(m));
        when(messageRepository.save(m)).thenReturn(m);

        MessageDto result = conversationService.editMessage("1", 10L, "New body");

        assertEquals("New body", result.body());
    }

    @Test
    void editMessageRejectsEmptyBody() {
        Exception ex = assertThrows(Exception.class, () -> conversationService.editMessage("1", 10L, " "));
        assertEquals("Message body cannot be empty.", ex.getMessage());
    }

    @Test
    void editMessageRejectsNonSender() {
        Message m = message(10L, 1L, 2L);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(m));

        Exception ex = assertThrows(Exception.class, () -> conversationService.editMessage("1", 10L, "New body"));
        assertEquals("You can only edit your own messages.", ex.getMessage());
    }

    @Test
    void editMessageRejectsDeletedMessage() {
        Message m = message(10L, 1L, 1L);
        m.setDeleted(true);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(m));

        Exception ex = assertThrows(Exception.class, () -> conversationService.editMessage("1", 10L, "New body"));
        assertEquals("Cannot edit a deleted message.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // deleteMessage
    // -------------------------------------------------------------------------

    @Test
    void deleteMessageSoftDeletes() throws Exception {
        Message m = message(10L, 1L, 1L);
        m.setBody("Hey!");
        when(messageRepository.findById(10L)).thenReturn(Optional.of(m));
        when(messageRepository.save(m)).thenReturn(m);

        MessageDto result = conversationService.deleteMessage("1", 10L);

        assertTrue(result.isDeleted());
        assertEquals("", result.body());
    }

    @Test
    void deleteMessageRejectsNonSender() {
        Message m = message(10L, 1L, 2L);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(m));

        Exception ex = assertThrows(Exception.class, () -> conversationService.deleteMessage("1", 10L));
        assertEquals("You can only delete your own messages.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // markRead
    // -------------------------------------------------------------------------

    @Test
    void markReadResetsUnreadCountForParticipantA() throws Exception {
        Conversation c = conversation(1L, 1L, 2L);
        c.setUnreadCountA(5);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(c));

        conversationService.markRead(1L, "1");

        assertEquals(0, c.getUnreadCountA());
        verify(messageRepository).markAllReadInConversation(eq(1L), eq(1L), any());
    }

    @Test
    void markReadRejectsNonParticipant() {
        Conversation c = conversation(1L, 1L, 2L);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(Exception.class, () -> conversationService.markRead(1L, "999"));
    }

    // -------------------------------------------------------------------------
    // deleteConversation
    // -------------------------------------------------------------------------

    @Test
    void deleteConversationMarksDeletedForParticipantB() throws Exception {
        Conversation c = conversation(1L, 1L, 2L);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(c));

        conversationService.deleteConversation(1L, "2");

        assertTrue(c.isDeletedByB());
    }

    @Test
    void deleteConversationRejectsNonParticipant() {
        Conversation c = conversation(1L, 1L, 2L);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(Exception.class, () -> conversationService.deleteConversation(1L, "999"));
    }
}
