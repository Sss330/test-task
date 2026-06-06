package test.task.bfg.model.dto.response;

import test.task.bfg.model.entity.Message;
import test.task.bfg.model.enums.MessageStatus;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        String senderUsername,
        UUID receiverId,
        String receiverUsername,
        String text,
        MessageStatus status,
        Instant createdAt,
        Instant deliveredAt,
        Instant readAt
) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getReceiver().getId(),
                message.getReceiver().getUsername(),
                message.getText(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getDeliveredAt(),
                message.getReadAt()
        );
    }
}