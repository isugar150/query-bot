package com.namejm.query_bot.controller;

import com.namejm.query_bot.dto.MetabaseQuestionRequest;
import com.namejm.query_bot.dto.MetabaseQuestionResponse;
import com.namejm.query_bot.dto.MetabaseStatusResponse;
import com.namejm.query_bot.service.MetabaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metabase")
public class MetabaseController {

    private final MetabaseService metabaseService;

    public MetabaseController(MetabaseService metabaseService) {
        this.metabaseService = metabaseService;
    }

    @GetMapping("/status")
    public MetabaseStatusResponse status() {
        return new MetabaseStatusResponse(metabaseService.isAvailable());
    }

    @PostMapping("/card")
    public ResponseEntity<MetabaseQuestionResponse> create(@Valid @RequestBody MetabaseQuestionRequest request) {
        if (!metabaseService.isAvailable()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(metabaseService.createOrUpdateCard(request));
    }
}
