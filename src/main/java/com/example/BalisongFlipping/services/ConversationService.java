package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.messagingDtos.ConversationDto;
import com.example.BalisongFlipping.dtos.messagingDtos.MessageDto;
import com.example.BalisongFlipping.enums.notifications.NotificationType;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.messaging.Conversation;
import com.example.BalisongFlipping.modals.messaging.Message;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.ConversationRepository;
import com.example.BalisongFlipping.repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ConversationService {

    private static final long DORMANT_THRESHOLD_MS = 30L * 24 * 60 * 60 * 1000;

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private EmailService emailService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // -------------------------------------------------------------------------
    // Inbox
    // -------------------------------------------------------------------------

    public List<ConversationDto> getInbox(String accountId) throws Exception {
        Long uid = Long.parseLong(accountId);
        return conversationRepository.findInboxForUser(uid).stream()
                .map(c -> toConversationDto(c, uid))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Message history
    // -------------------------------------------------------------------------

    public Page<MessageDto> getMessages(Long conversationId, String accountId, int page, int size) throws Exception {
        Long uid = Long.parseLong(accountId);
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new Exception("Conversation not found."));
        assertParticipant(conv, uid);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        return messageRepository.findByConversationId(conversationId, pageable)
                .map(this::toMessageDto);
    }

    // -------------------------------------------------------------------------
    // Send message
    // -------------------------------------------------------------------------

    @Transactional
    public MessageDto sendMessage(String senderAccountId, String recipientAccountId, String body,
                                  String mediaUrl, boolean isVideo, Long replyToId) throws Exception {
        boolean hasBody  = body != null && !body.isBlank();
        boolean hasMedia = mediaUrl != null;
        if (!hasBody && !hasMedia) throw new Exception("A message must contain text or a media file.");
        if (hasBody && body.length() > 2000) throw new Exception("Message body may not exceed 2000 characters.");
        if (senderAccountId.equals(recipientAccountId)) throw new Exception("Cannot message yourself.");

        Long senderId    = Long.parseLong(senderAccountId);
        Long recipientId = Long.parseLong(recipientAccountId);

        User recipient = accountRepository.findById(recipientId)
                .map(a -> (User) a)
                .orElseThrow(() -> new Exception("Recipient not found."));

        Long aId = Math.min(senderId, recipientId);
        Long bId = Math.max(senderId, recipientId);
        boolean senderIsA = senderId.equals(aId);

        Conversation conv = conversationRepository.findByParticipantAIdAndParticipantBId(aId, bId)
                .orElse(null);

        boolean isNewConversation = (conv == null || conv.getLastMessageAt() == null);
        boolean isDormant = !isNewConversation &&
                (new Date().getTime() - conv.getLastMessageAt().getTime()) > DORMANT_THRESHOLD_MS;

        if (conv == null) {
            conv = new Conversation();
            conv.setParticipantAId(aId);
            conv.setParticipantBId(bId);
        }

        conv.setDeletedByA(false);
        conv.setDeletedByB(false);

        String previewText = hasBody ? body : (isVideo ? "[Video]" : "[Photo]");
        String preview = previewText.length() <= 100 ? previewText : previewText.substring(0, 97) + "...";
        conv.setLastMessagePreview(preview);
        conv.setLastMessageAt(new Date());

        if (senderIsA) {
            conv.setUnreadCountB(conv.getUnreadCountB() + 1);
        } else {
            conv.setUnreadCountA(conv.getUnreadCountA() + 1);
        }

        Conversation savedConv = conversationRepository.save(conv);

        if (replyToId != null) {
            Message replied = messageRepository.findById(replyToId)
                    .orElseThrow(() -> new Exception("Replied-to message not found."));
            if (!replied.getConversationId().equals(savedConv.getId())) {
                throw new Exception("Cannot reply to a message from a different conversation.");
            }
        }

        Message msg = new Message();
        msg.setConversationId(savedConv.getId());
        msg.setSenderId(senderId);
        msg.setBody(hasBody ? body : "");
        msg.setMediaUrl(mediaUrl);
        msg.setVideo(isVideo);
        msg.setReplyToId(replyToId);
        msg.setSentAt(new Date());
        Message savedMsg = messageRepository.save(msg);

        MessageDto messageDto = toMessageDto(savedMsg);
        ConversationDto convDto = toConversationDto(savedConv, recipientId);

        String recipientEmail = recipient.getEmail();
        messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/messages", messageDto);
        messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/conversations", convDto);

        notificationService.send(recipientId, senderId, NotificationType.MESSAGE_RECEIVED,
                TargetType.CONVERSATION, savedConv.getId());

        if (isNewConversation || isDormant) {
            User sender = (User) accountRepository.findById(senderId).orElse(null);
            String senderName = sender != null ? sender.getDisplayName() : "Someone";
            try {
                emailService.sendEmail(
                        recipientEmail,
                        "New message from " + senderName,
                        senderName + " sent you a message:\n\n\"" + preview + "\"\n\nOpen the app to reply."
                );
            } catch (Exception ignored) {}
        }

        return messageDto;
    }

    // -------------------------------------------------------------------------
    // Edit message
    // -------------------------------------------------------------------------

    @Transactional
    public MessageDto editMessage(String accountId, Long messageId, String newBody) throws Exception {
        if (newBody == null || newBody.isBlank()) throw new Exception("Message body cannot be empty.");
        if (newBody.length() > 2000) throw new Exception("Message body may not exceed 2000 characters.");

        Long uid = Long.parseLong(accountId);
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new Exception("Message not found."));
        if (!msg.getSenderId().equals(uid)) throw new Exception("You can only edit your own messages.");
        if (msg.isDeleted()) throw new Exception("Cannot edit a deleted message.");

        msg.setBody(newBody.trim());
        msg.setEditedAt(new Date());
        return toMessageDto(messageRepository.save(msg));
    }

    // -------------------------------------------------------------------------
    // Delete message (soft)
    // -------------------------------------------------------------------------

    @Transactional
    public MessageDto deleteMessage(String accountId, Long messageId) throws Exception {
        Long uid = Long.parseLong(accountId);
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new Exception("Message not found."));
        if (!msg.getSenderId().equals(uid)) throw new Exception("You can only delete your own messages.");

        msg.setDeleted(true);
        msg.setBody("");
        msg.setMediaUrl(null);
        return toMessageDto(messageRepository.save(msg));
    }

    // -------------------------------------------------------------------------
    // Mark conversation read
    // -------------------------------------------------------------------------

    @Transactional
    public void markRead(Long conversationId, String accountId) throws Exception {
        Long uid = Long.parseLong(accountId);
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new Exception("Conversation not found."));
        assertParticipant(conv, uid);

        messageRepository.markAllReadInConversation(conversationId, uid, new Date());

        if (uid.equals(conv.getParticipantAId())) {
            conv.setUnreadCountA(0);
        } else {
            conv.setUnreadCountB(0);
        }
        conversationRepository.save(conv);
    }

    // -------------------------------------------------------------------------
    // Delete conversation (soft)
    // -------------------------------------------------------------------------

    @Transactional
    public void deleteConversation(Long conversationId, String accountId) throws Exception {
        Long uid = Long.parseLong(accountId);
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new Exception("Conversation not found."));
        assertParticipant(conv, uid);

        if (uid.equals(conv.getParticipantAId())) {
            conv.setDeletedByA(true);
        } else {
            conv.setDeletedByB(true);
        }
        conversationRepository.save(conv);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void assertParticipant(Conversation conv, Long uid) throws Exception {
        if (!uid.equals(conv.getParticipantAId()) && !uid.equals(conv.getParticipantBId())) {
            throw new Exception("You are not a participant in this conversation.");
        }
    }

    private ConversationDto toConversationDto(Conversation c, Long requesterId) {
        Long otherId = c.getParticipantAId().equals(requesterId) ? c.getParticipantBId() : c.getParticipantAId();
        int unread = c.getParticipantAId().equals(requesterId) ? c.getUnreadCountA() : c.getUnreadCountB();

        User other = accountRepository.findById(otherId).map(a -> (User) a).orElse(null);
        String displayName   = other != null ? other.getDisplayName()    : "[deleted]";
        String identifierCode = other != null ? other.getIdentifierCode() : null;
        String profileImg    = other != null ? other.getProfileImg()     : null;

        return new ConversationDto(
                c.getId(),
                otherId.toString(),
                displayName,
                identifierCode,
                profileImg,
                c.getLastMessagePreview(),
                c.getLastMessageAt(),
                unread
        );
    }

    private MessageDto toMessageDto(Message m) {
        String replyPreviewBody       = null;
        String replyPreviewSenderName = null;

        if (m.getReplyToId() != null) {
            Message reply = messageRepository.findById(m.getReplyToId()).orElse(null);
            if (reply != null) {
                if (reply.isDeleted()) {
                    replyPreviewBody = "";
                } else if (reply.getBody() != null && !reply.getBody().isBlank()) {
                    String b = reply.getBody();
                    replyPreviewBody = b.length() > 60 ? b.substring(0, 57) + "..." : b;
                } else if (reply.getMediaUrl() != null) {
                    replyPreviewBody = reply.isVideo() ? "[Video]" : "[Photo]";
                }
                Account senderAcc = accountRepository.findById(reply.getSenderId()).orElse(null);
                if (senderAcc instanceof User u) {
                    replyPreviewSenderName = u.getDisplayName();
                }
            }
        }

        return new MessageDto(
                m.getId(),
                m.getConversationId(),
                m.getSenderId().toString(),
                m.getBody(),
                m.getMediaUrl(),
                m.isVideo(),
                m.getReplyToId(),
                replyPreviewBody,
                replyPreviewSenderName,
                m.getSentAt(),
                m.getEditedAt(),
                m.isDeleted(),
                m.getReadAt()
        );
    }
}
