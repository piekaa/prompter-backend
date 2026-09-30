package com.prompter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScriptRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull String content
) {
}
