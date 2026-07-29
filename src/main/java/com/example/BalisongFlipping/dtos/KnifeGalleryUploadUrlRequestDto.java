package com.example.BalisongFlipping.dtos;

import com.example.BalisongFlipping.dtos.uploadsDtos.FileUploadRequestItem;

import java.util.List;

public record KnifeGalleryUploadUrlRequestDto(String displayName, List<FileUploadRequestItem> files) {}
