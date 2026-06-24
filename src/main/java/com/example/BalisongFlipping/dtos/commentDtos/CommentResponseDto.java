package com.example.BalisongFlipping.dtos.commentDtos;

import com.example.BalisongFlipping.modals.comments.Comment;

public record CommentResponseDto(
        Comment comment,
        CommentAuthorDto author
) {}
