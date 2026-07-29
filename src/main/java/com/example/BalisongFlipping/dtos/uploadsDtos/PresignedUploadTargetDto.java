package com.example.BalisongFlipping.dtos.uploadsDtos;

public record PresignedUploadTargetDto(String key, String uploadUrl, String publicUrl, boolean isVideo) {}
