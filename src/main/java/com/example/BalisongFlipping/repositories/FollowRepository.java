package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.follows.Follow;
import com.example.BalisongFlipping.modals.follows.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
}
