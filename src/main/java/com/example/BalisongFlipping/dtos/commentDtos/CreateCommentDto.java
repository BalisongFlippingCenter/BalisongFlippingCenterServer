package com.example.BalisongFlipping.dtos.commentDtos;

public record CreateCommentDto(
        String content,
        Long parentCommentId
) {}
