package test.task.bfg.model.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank
        @Size(max = 64)
        String username,

        @NotBlank
        @Size(max = 128)
        String displayName
) {
}