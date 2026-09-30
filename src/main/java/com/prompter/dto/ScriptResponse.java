package com.prompter.dto;

import com.prompter.model.Script;

import java.time.LocalDateTime;

public record ScriptResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScriptResponse from(Script s) {
        return new ScriptResponse(s.getId(), s.getTitle(), s.getContent(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
