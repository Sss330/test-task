package test.task.bfg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import test.task.bfg.exception.BadRequestException;
import test.task.bfg.exception.NotFoundException;
import test.task.bfg.model.dto.request.SendMessageRequest;
import test.task.bfg.model.dto.response.MessageResponse;
import test.task.bfg.model.entity.Message;
import test.task.bfg.model.entity.User;
import test.task.bfg.model.enums.MessageStatus;
import test.task.bfg.repository.MessageRepository;
import test.task.bfg.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SseNotificationService notificationService;

    @Transactional
    public MessageResponse send(SendMessageRequest request) {
        if (request.senderId().equals(request.receiverId())) {
            throw new BadRequestException("Sender and receiver must be different users");
        }

        User sender = getUserOrThrow(request.senderId());
        User receiver = getUserOrThrow(request.receiverId());

        Message message = new Message(sender, receiver, request.text());

        if (notificationService.hasSubscribers(receiver.getId())) {
            message.markDelivered();
        }

        Message savedMessage = messageRepository.save(message);
        MessageResponse response = MessageResponse.from(savedMessage);

        notificationService.send(receiver.getId(), "message-received", response);
        notificationService.send(sender.getId(), "message-status-updated", response);

        return response;
    }

    @Transactional(readOnly = true)
    public MessageResponse findById(UUID messageId) {
        return messageRepository.findById(messageId)
                .map(MessageResponse::from)
                .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> findConversation(UUID firstUserId, UUID secondUserId) {
        getUserOrThrow(firstUserId);
        getUserOrThrow(secondUserId);

        return messageRepository.findConversation(firstUserId, secondUserId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse markAsRead(UUID messageId, UUID readerId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));

        if (!message.getReceiver().getId().equals(readerId)) {
            throw new BadRequestException("Only receiver can mark message as read");
        }

        message.markRead();

        Message savedMessage = messageRepository.save(message);
        MessageResponse response = MessageResponse.from(savedMessage);

        notificationService.send(message.getSender().getId(), "message-status-updated", response);
        notificationService.send(message.getReceiver().getId(), "message-status-updated", response);

        return response;
    }

    @Transactional
    public SseEmitter subscribe(UUID userId) {
        getUserOrThrow(userId);

        SseEmitter emitter = notificationService.subscribe(userId);

        List<Message> pendingMessages = messageRepository.findByReceiver_IdAndStatus(
                userId,
                MessageStatus.SENT
        );

        for (Message message : pendingMessages) {
            message.markDelivered();

            MessageResponse response = MessageResponse.from(message);

            notificationService.send(userId, "message-received", response);
            notificationService.send(message.getSender().getId(), "message-status-updated", response);
        }

        return emitter;
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}