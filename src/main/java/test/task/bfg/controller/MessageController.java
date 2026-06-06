package test.task.bfg.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import test.task.bfg.model.dto.request.SendMessageRequest;
import test.task.bfg.model.dto.response.MessageResponse;
import test.task.bfg.service.MessageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@RequestBody @Valid SendMessageRequest request) {
        return messageService.send(request);
    }

    @GetMapping("/{messageId}")
    public MessageResponse findById(@PathVariable UUID messageId) {
        return messageService.findById(messageId);
    }

    @GetMapping("/conversation")
    public List<MessageResponse> findConversation(
            @RequestParam UUID firstUserId,
            @RequestParam UUID secondUserId
    ) {
        return messageService.findConversation(firstUserId, secondUserId);
    }

    @PatchMapping("/{messageId}/read")
    public MessageResponse markAsRead(
            @PathVariable UUID messageId,
            @RequestParam UUID readerId
    ) {
        return messageService.markAsRead(messageId, readerId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam UUID userId) {
        return messageService.subscribe(userId);
    }
}