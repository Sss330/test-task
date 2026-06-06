package test.task.bfg.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import test.task.bfg.model.enums.MessageStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 2000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MessageStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant deliveredAt;

    private Instant readAt;

    public Message(User sender, User receiver, String text) {
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
        this.status = MessageStatus.SENT;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void markDelivered() {
        if (status == MessageStatus.SENT) {
            status = MessageStatus.DELIVERED;
            deliveredAt = Instant.now();
        }
    }

    public void markRead() {
        if (status != MessageStatus.READ) {
            status = MessageStatus.READ;
            readAt = Instant.now();

            if (deliveredAt == null) {
                deliveredAt = Instant.now();
            }
        }
    }
}