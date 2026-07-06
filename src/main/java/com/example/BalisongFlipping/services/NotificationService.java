package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.notificationDtos.NotificationDto;
import com.example.BalisongFlipping.enums.notifications.NotificationType;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.notifications.Notification;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // -------------------------------------------------------------------------
    // Create + push (called internally by other services)
    // -------------------------------------------------------------------------

    public void send(Long recipientId, Long actorId, NotificationType type, TargetType targetType, Long targetId) {
        // Never notify someone about their own actions
        if (recipientId.equals(actorId)) return;

        User actor = accountRepository.findById(actorId)
                .map(a -> (User) a)
                .orElse(null);

        Notification notification = new Notification();
        notification.setRecipientAccountId(recipientId);
        notification.setActorAccountId(actorId);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);

        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = toDto(saved, actor);

        // Push to recipient if connected — silently ignored if offline
        String recipientEmail = accountRepository.findById(recipientId)
                .map(a -> a.getEmail())
                .orElse(null);
        if (recipientEmail != null) {
            messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/notifications", dto);
        }
    }

    // -------------------------------------------------------------------------
    // REST reads
    // -------------------------------------------------------------------------

    public Page<NotificationDto> getNotifications(Long accountId, boolean unreadOnly, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> results = unreadOnly
                ? notificationRepository.findByRecipientAccountIdAndIsRead(accountId, false, pageable)
                : notificationRepository.findByRecipientAccountId(accountId, pageable);

        return results.map(n -> {
            User actor = n.getActorAccountId() != null
                    ? accountRepository.findById(n.getActorAccountId()).map(a -> (User) a).orElse(null)
                    : null;
            return toDto(n, actor);
        });
    }

    public long getUnreadCount(Long accountId) {
        return notificationRepository.countByRecipientAccountIdAndIsRead(accountId, false);
    }

    // -------------------------------------------------------------------------
    // Mark read
    // -------------------------------------------------------------------------

    @Transactional
    public NotificationDto markRead(Long notificationId, Long accountId) throws Exception {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new Exception("Notification not found."));

        if (!notification.getRecipientAccountId().equals(accountId))
            throw new Exception("Not your notification.");

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);

        User actor = saved.getActorAccountId() != null
                ? accountRepository.findById(saved.getActorAccountId()).map(a -> (User) a).orElse(null)
                : null;
        return toDto(saved, actor);
    }

    @Transactional
    public void markAllRead(Long accountId) {
        notificationRepository.markAllReadForRecipient(accountId);
    }

    // -------------------------------------------------------------------------
    // DTO builder
    // -------------------------------------------------------------------------

    private NotificationDto toDto(Notification n, User actor) {
        String displayName = actor != null ? actor.getDisplayName() : "[deleted]";
        String identifierCode = actor != null ? actor.getIdentifierCode() : null;
        String profileImg = actor != null ? actor.getProfileImg() : null;

        String message = buildMessage(n.getType(), displayName);

        return new NotificationDto(
                n.getId(),
                n.getType(),
                message,
                n.getTargetType(),
                n.getTargetId(),
                displayName,
                identifierCode,
                profileImg,
                n.isRead(),
                n.getCreatedAt()
        );
    }

    private String buildMessage(NotificationType type, String actorName) {
        return switch (type) {
            case NEW_FOLLOWER    -> actorName + " started following you";
            case POST_LIKED      -> actorName + " liked your post";
            case POST_COMMENTED  -> actorName + " commented on your post";
            case COMMENT_REPLIED -> actorName + " replied to your comment";
            case COMMENT_LIKED   -> actorName + " liked your comment";
        };
    }
}
