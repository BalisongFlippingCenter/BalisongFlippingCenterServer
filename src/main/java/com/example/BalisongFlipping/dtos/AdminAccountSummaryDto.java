package com.example.BalisongFlipping.dtos;

import java.time.Instant;
import java.util.Date;

public record AdminAccountSummaryDto(
        String id,
        String email,
        String displayName,
        String identifierCode,
        String role,
        Date accountCreationDate,
        boolean banned,
        String banReason,
        Instant suspendedUntil,
        String suspendReason,
        Instant mutedUntil,
        String muteReason
) {}
