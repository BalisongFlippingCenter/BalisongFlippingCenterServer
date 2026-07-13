package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.messaging.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByParticipantAIdAndParticipantBId(Long aId, Long bId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE (c.participantAId = :uid AND c.deletedByA = false)
               OR (c.participantBId = :uid AND c.deletedByB = false)
            ORDER BY c.lastMessageAt DESC NULLS LAST
            """)
    List<Conversation> findInboxForUser(@Param("uid") Long userId);
}
