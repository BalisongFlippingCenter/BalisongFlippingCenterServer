package com.example.BalisongFlipping.modals.messaging;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_a_id", nullable = false)
    private Long participantAId;

    @Column(name = "participant_b_id", nullable = false)
    private Long participantBId;

    @Column(name = "last_message_at")
    private Date lastMessageAt;

    @Column(name = "last_message_preview", length = 100)
    private String lastMessagePreview;

    @Column(name = "unread_count_a")
    private int unreadCountA = 0;

    @Column(name = "unread_count_b")
    private int unreadCountB = 0;

    @Column(name = "deleted_by_a")
    private boolean deletedByA = false;

    @Column(name = "deleted_by_b")
    private boolean deletedByB = false;

    public Conversation() {}

    public Long getId() { return id; }
    public Long getParticipantAId() { return participantAId; }
    public void setParticipantAId(Long participantAId) { this.participantAId = participantAId; }
    public Long getParticipantBId() { return participantBId; }
    public void setParticipantBId(Long participantBId) { this.participantBId = participantBId; }
    public Date getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Date lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }
    public int getUnreadCountA() { return unreadCountA; }
    public void setUnreadCountA(int unreadCountA) { this.unreadCountA = unreadCountA; }
    public int getUnreadCountB() { return unreadCountB; }
    public void setUnreadCountB(int unreadCountB) { this.unreadCountB = unreadCountB; }
    public boolean isDeletedByA() { return deletedByA; }
    public void setDeletedByA(boolean deletedByA) { this.deletedByA = deletedByA; }
    public boolean isDeletedByB() { return deletedByB; }
    public void setDeletedByB(boolean deletedByB) { this.deletedByB = deletedByB; }
}
