package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.messaging.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationId(Long conversationId, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :now WHERE m.conversationId = :convId AND m.senderId <> :readerId AND m.readAt IS NULL")
    int markAllReadInConversation(@Param("convId") Long convId, @Param("readerId") Long readerId, @Param("now") Date now);
}
