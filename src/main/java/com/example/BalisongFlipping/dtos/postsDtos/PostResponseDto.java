package com.example.BalisongFlipping.dtos.postsDtos;

import com.example.BalisongFlipping.modals.posts.PostWrapper;

public record PostResponseDto(
        PostWrapper post,
        PostAuthorDto author,
        PostKnifeDto offeringKnife
) {}
