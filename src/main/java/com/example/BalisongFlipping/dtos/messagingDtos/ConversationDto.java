package com.example.BalisongFlipping.dtos.messagingDtos;

import java.util.Date;

public record ConversationDto(
        Long id,
        String otherParticipantId,
        String otherDisplayName,
        String otherIdentifierCode,
        String otherProfileImg,
        String lastMessagePreview,
        Date lastMessageAt,
        int unreadCount
) {}
