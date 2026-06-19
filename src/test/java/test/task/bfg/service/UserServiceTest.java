package test.task.bfg.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import test.task.bfg.exception.ConflictException;
import test.task.bfg.exception.NotFoundException;
import test.task.bfg.model.dto.request.CreateUserRequest;
import test.task.bfg.model.dto.response.UserResponse;
import test.task.bfg.model.entity.User;
import test.task.bfg.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createShouldCreateUser() {
        CreateUserRequest request = new CreateUserRequest("ivan", "Ivan Ivanov");

        when(userRepository.existsByUsername("ivan")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(request);

        assertThat(response.getUsername()).isEqualTo("ivan");
        assertThat(response.getDisplayName()).isEqualTo("Ivan Ivanov");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createShouldThrowConflictExceptionWhenUsernameExists() {
        CreateUserRequest request = new CreateUserRequest("ivan", "Ivan Ivanov");

        when(userRepository.existsByUsername("ivan")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByIdShouldThrowNotFoundExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }
}