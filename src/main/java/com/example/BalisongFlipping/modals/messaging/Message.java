package com.example.BalisongFlipping.modals.messaging;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "sent_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;

    @Column(name = "read_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date readAt;

    public Message() {}

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
    public Date getReadAt() { return readAt; }
    public void setReadAt(Date readAt) { this.readAt = readAt; }
}
