package com.example.BalisongFlipping.dtos.messagingDtos;

import java.util.Date;

public record MessageDto(
        Long id,
        Long conversationId,
        String senderId,
        String body,
        String mediaUrl,
        boolean isVideo,
        Long replyToId,
        String replyPreviewBody,
        String replyPreviewSenderName,
        Date sentAt,
        Date editedAt,
        boolean isDeleted,
        Date readAt
) {}
