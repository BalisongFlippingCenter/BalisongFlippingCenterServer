package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.follows.Follow;
import com.example.BalisongFlipping.modals.follows.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    // All accounts this user follows (followerId = the viewer)
    List<Follow> findByIdFollowerId(Long followerId);

    // All accounts following this user (followingId = the profile being viewed)
    List<Follow> findByIdFollowingId(Long followingId);
}
