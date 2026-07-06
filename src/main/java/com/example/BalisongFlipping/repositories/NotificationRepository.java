package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.notifications.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientAccountId(Long recipientAccountId, Pageable pageable);

    Page<Notification> findByRecipientAccountIdAndIsRead(Long recipientAccountId, boolean isRead, Pageable pageable);

    long countByRecipientAccountIdAndIsRead(Long recipientAccountId, boolean isRead);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientAccountId = :recipientId AND n.isRead = false")
    void markAllReadForRecipient(@Param("recipientId") Long recipientId);
}
