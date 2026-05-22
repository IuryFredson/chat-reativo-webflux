package com.iury.chat.message;

public record ChatInboundEvent(
        ChatEventType type,
        String author,
        String content,
        Boolean typing
) {
}
