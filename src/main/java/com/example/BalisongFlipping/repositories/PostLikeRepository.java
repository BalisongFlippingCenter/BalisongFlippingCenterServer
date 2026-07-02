package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.posts.PostLike;
import com.example.BalisongFlipping.modals.posts.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    void deleteAllById_AccountId(Long accountId);
}
