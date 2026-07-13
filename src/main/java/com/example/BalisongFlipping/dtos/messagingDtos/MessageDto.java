package com.example.BalisongFlipping.dtos.messagingDtos;

import java.util.Date;

public record MessageDto(
        Long id,
        Long conversationId,
        String senderId,
        String body,
        Date sentAt,
        Date readAt
) {}
