package com.iury.chat.message;

import java.time.Instant;

public record ChatMessageResponse(
        String id,
        String room,
        String author,
        String content,
        Instant sentAt
) {
    static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom(),
                message.getAuthor(),
                message.getContent(),
                message.getSentAt()
        );
    }
}
