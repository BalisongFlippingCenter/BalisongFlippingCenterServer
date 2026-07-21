package com.example.BalisongFlipping.dtos.messagingDtos;

import java.util.Date;

public record MessageDto(
        Long id,
        Long conversationId,
        String senderId,
        String body,
        String mediaUrl,
        boolean isVideo,
        Date sentAt,
        Date readAt
) {}
