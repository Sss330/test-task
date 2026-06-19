package test.task.bfg.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import test.task.bfg.exception.BadRequestException;
import test.task.bfg.exception.ForbiddenException;
import test.task.bfg.model.dto.request.SendMessageRequest;
import test.task.bfg.model.dto.response.MessageResponse;
import test.task.bfg.model.entity.Message;
import test.task.bfg.model.entity.User;
import test.task.bfg.model.enums.MessageStatus;
import test.task.bfg.repository.MessageRepository;
import test.task.bfg.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SseNotificationService notificationService;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendShouldCreateMessage() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        User sender = user(senderId, "ivan", "Ivan Ivanov");
        User receiver = user(receiverId, "petr", "Petr Petrov");

        SendMessageRequest request = new SendMessageRequest(
                senderId,
                receiverId,
                "Hello!"
        );

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(notificationService.hasSubscribers(receiverId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = messageService.send(senderId, request);

        assertThat(response.senderId()).isEqualTo(senderId);
        assertThat(response.receiverId()).isEqualTo(receiverId);
        assertThat(response.text()).isEqualTo("Hello!");
        assertThat(response.status()).isEqualTo(MessageStatus.DELIVERED);

        verify(messageRepository).save(any(Message.class));
        verify(notificationService).send(eq(receiverId), eq("message-received"), any(MessageResponse.class));
        verify(notificationService).send(eq(senderId), eq("message-status-updated"), any(MessageResponse.class));
    }

    @Test
    void sendShouldThrowForbiddenExceptionWhenCurrentUserIsNotSender() {
        UUID currentUserId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        SendMessageRequest request = new SendMessageRequest(
                senderId,
                receiverId,
                "Hello!"
        );

        assertThatThrownBy(() -> messageService.send(currentUserId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Current user cannot send message on behalf of another user");

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendShouldThrowBadRequestExceptionWhenSenderSendsMessageToHimself() {
        UUID userId = UUID.randomUUID();

        SendMessageRequest request = new SendMessageRequest(
                userId,
                userId,
                "Hello myself!"
        );

        assertThatThrownBy(() -> messageService.send(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Sender and receiver must be different users");

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void markAsReadShouldAllowReceiverToMarkMessageAsRead() {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        User sender = user(senderId, "ivan", "Ivan Ivanov");
        User receiver = user(receiverId, "petr", "Petr Petrov");

        Message message = new Message(sender, receiver, "Hello!");

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(message)).thenReturn(message);

        MessageResponse response = messageService.markAsRead(receiverId, messageId, receiverId);

        assertThat(response.status()).isEqualTo(MessageStatus.READ);
        assertThat(response.readAt()).isNotNull();
        assertThat(response.deliveredAt()).isNotNull();

        verify(messageRepository).save(message);
        verify(notificationService).send(eq(senderId), eq("message-status-updated"), any(MessageResponse.class));
        verify(notificationService).send(eq(receiverId), eq("message-status-updated"), any(MessageResponse.class));
    }

    @Test
    void markAsReadShouldThrowForbiddenExceptionWhenCurrentUserIsNotReader() {
        UUID currentUserId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID readerId = UUID.randomUUID();

        assertThatThrownBy(() -> messageService.markAsRead(currentUserId, messageId, readerId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Current user cannot mark message as read on behalf of another user");

        verify(messageRepository, never()).findById(any(UUID.class));
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void markAsReadShouldThrowBadRequestExceptionWhenReaderIsNotReceiver() {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();

        User sender = user(senderId, "ivan", "Ivan Ivanov");
        User receiver = user(receiverId, "petr", "Petr Petrov");

        Message message = new Message(sender, receiver, "Hello!");

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.markAsRead(anotherUserId, messageId, anotherUserId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only receiver can mark message as read");

        verify(messageRepository, never()).save(any(Message.class));
    }

    private User user(UUID id, String username, String displayName) {
        User user = new User(username, displayName);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}