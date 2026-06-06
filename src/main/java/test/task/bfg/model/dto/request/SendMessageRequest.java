package test.task.bfg.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(

        @NotNull
        UUID senderId,

        @NotNull
        UUID receiverId,

        @NotBlank
        @Size(max = 2000)
        String text
) {
}