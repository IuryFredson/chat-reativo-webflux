package com.iury.chat.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank @Size(max = 40) String author,
        @NotBlank @Size(max = 500) String content
) {
}
