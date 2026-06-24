package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.comments.CommentLike;
import com.example.BalisongFlipping.modals.comments.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {}
