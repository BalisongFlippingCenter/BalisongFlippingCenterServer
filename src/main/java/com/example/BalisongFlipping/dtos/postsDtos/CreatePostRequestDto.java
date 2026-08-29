package com.example.BalisongFlipping.dtos.postsDtos;

import java.util.List;

public record CreatePostRequestDto(
        String postType,
        String caption,
        String description,
        String referenceKnifeId,
        List<PostMediaInputDto> media,
        String mode,
        String offeringKnifeId,
        String price,
        String lookingForText,
        List<String> tags,
        String difficultyTag,
        List<String> techniqueTags
) {}
