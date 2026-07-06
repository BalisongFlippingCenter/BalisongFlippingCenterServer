package com.example.BalisongFlipping.dtos.notificationDtos;

import com.example.BalisongFlipping.enums.notifications.NotificationType;
import com.example.BalisongFlipping.enums.reports.TargetType;

import java.time.Instant;

public record NotificationDto(
        Long id,
        NotificationType type,
        String message,
        TargetType targetType,
        Long targetId,
        String actorDisplayName,
        String actorIdentifierCode,
        String actorProfileImg,
        boolean isRead,
        Instant createdAt
) {}
