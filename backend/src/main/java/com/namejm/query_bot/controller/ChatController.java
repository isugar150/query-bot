package com.namejm.query_bot.controller;

import com.namejm.query_bot.dto.ChatRequest;
import com.namejm.query_bot.dto.ChatResponse;
import com.namejm.query_bot.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    public ChatResponse ask(@Valid @RequestBody ChatRequest request) throws Exception {
        return chatService.ask(request);
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ChatResponse> history(@PathVariable Long sessionId) {
        return chatService.history(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    public ResponseEntity<ChatResponse> latest(@RequestParam("dbId") Long dbId) {
        return chatService.latestForDatabase(dbId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
