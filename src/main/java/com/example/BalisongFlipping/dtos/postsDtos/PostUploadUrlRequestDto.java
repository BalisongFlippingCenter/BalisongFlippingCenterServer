package com.example.BalisongFlipping.dtos.postsDtos;

import com.example.BalisongFlipping.dtos.uploadsDtos.FileUploadRequestItem;

import java.util.List;

public record PostUploadUrlRequestDto(String postType, List<FileUploadRequestItem> files) {}
