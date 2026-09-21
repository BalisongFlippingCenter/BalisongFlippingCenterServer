package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.notificationDtos.NotificationDto;
import com.example.BalisongFlipping.enums.notifications.NotificationType;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.notifications.Notification;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
        ReflectionTestUtils.setField(notificationService, "notificationRepository", notificationRepository);
        ReflectionTestUtils.setField(notificationService, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(notificationService, "messagingTemplate", messagingTemplate);
    }

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("flipper" + id + "@example.com");
        u.setDisplayName("Flipper" + id);
        return u;
    }

    private Notification notification(Long id, Long recipientId, Long actorId, NotificationType type) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientAccountId(recipientId);
        n.setActorAccountId(actorId);
        n.setType(type);
        n.setTargetType(TargetType.POST);
        n.setTargetId(5L);
        return n;
    }

    // -------------------------------------------------------------------------
    // send
    // -------------------------------------------------------------------------

    @Test
    void sendSkipsSelfNotification() {
        notificationService.send(1L, 1L, NotificationType.POST_LIKED, TargetType.POST, 5L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendCreatesNotificationAndPushesToConnectedRecipient() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(10L);
            return n;
        });

        notificationService.send(1L, 2L, NotificationType.POST_LIKED, TargetType.POST, 5L);

        verify(messagingTemplate).convertAndSendToUser(eq("flipper1@example.com"), eq("/queue/notifications"), any(NotificationDto.class));
    }

    @Test
    void sendBuildsHumanReadableMessagePerType() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.send(1L, 2L, NotificationType.NEW_FOLLOWER, TargetType.PROFILE, 2L);

        var captor = org.mockito.ArgumentCaptor.forClass(NotificationDto.class);
        verify(messagingTemplate).convertAndSendToUser(eq("flipper1@example.com"), eq("/queue/notifications"), captor.capture());
        assertEquals("Flipper2 started following you", captor.getValue().message());
    }

    @Test
    void sendSkipsPushWhenRecipientHasNoAccount() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.send(1L, 2L, NotificationType.POST_LIKED, TargetType.POST, 5L);

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // getNotifications / getUnreadCount
    // -------------------------------------------------------------------------

    @Test
    void getNotificationsReturnsAllWhenNotUnreadOnly() {
        when(notificationRepository.findByRecipientAccountId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(notification(1L, 1L, 2L, NotificationType.POST_LIKED))));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

        var result = notificationService.getNotifications(1L, false, 0, 20);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getNotificationsReturnsUnreadOnlyWhenRequested() {
        when(notificationRepository.findByRecipientAccountIdAndIsRead(eq(1L), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(notification(1L, 1L, 2L, NotificationType.POST_LIKED))));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

        var result = notificationService.getNotifications(1L, true, 0, 20);

        assertEquals(1, result.getContent().size());
        verify(notificationRepository, never()).findByRecipientAccountId(any(), any());
    }

    @Test
    void getNotificationsUsesDeletedPlaceholderWhenActorGone() {
        when(notificationRepository.findByRecipientAccountId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(notification(1L, 1L, 999L, NotificationType.POST_LIKED))));
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        var result = notificationService.getNotifications(1L, false, 0, 20);

        assertTrue(result.getContent().get(0).message().startsWith("[deleted]"));
    }

    @Test
    void getUnreadCountDelegatesToRepository() {
        when(notificationRepository.countByRecipientAccountIdAndIsRead(1L, false)).thenReturn(3L);
        assertEquals(3L, notificationService.getUnreadCount(1L));
    }

    // -------------------------------------------------------------------------
    // markRead / markAllRead
    // -------------------------------------------------------------------------

    @Test
    void markReadSucceeds() throws Exception {
        Notification n = notification(1L, 1L, 2L, NotificationType.POST_LIKED);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(n)).thenReturn(n);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

        NotificationDto result = notificationService.markRead(1L, 1L);

        assertTrue(result.isRead());
    }

    @Test
    void markReadRejectsNonOwner() {
        Notification n = notification(1L, 1L, 2L, NotificationType.POST_LIKED);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        Exception ex = assertThrows(Exception.class, () -> notificationService.markRead(1L, 999L));
        assertEquals("Not your notification.", ex.getMessage());
    }

    @Test
    void markReadThrowsForUnknownNotification() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> notificationService.markRead(999L, 1L));
    }

    @Test
    void markAllReadDelegatesToRepository() {
        notificationService.markAllRead(1L);
        verify(notificationRepository).markAllReadForRecipient(1L);
    }

    // -------------------------------------------------------------------------
    // sendSystem
    // -------------------------------------------------------------------------

    @Test
    void sendSystemCreatesNotificationWithNoActorAndPushes() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.sendSystem(1L, NotificationType.NAME_RESET, TargetType.PROFILE, 1L);

        var captor = org.mockito.ArgumentCaptor.forClass(NotificationDto.class);
        verify(messagingTemplate).convertAndSendToUser(eq("flipper1@example.com"), eq("/queue/notifications"), captor.capture());
        assertEquals("Your display name was reset for violating community guidelines", captor.getValue().message());
    }
}
