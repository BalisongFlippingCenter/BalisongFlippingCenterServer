package com.example.BalisongFlipping.dtos;

// until is an ISO-8601 instant string (e.g. "2026-09-22T00:00:00Z")
public record MuteAccountDto(String reason, String until) {}
