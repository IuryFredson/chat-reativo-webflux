package com.iury.chat.message;

import java.time.Instant;
import java.util.List;

public record ChatOutboundEvent(
        ChatEventType type,
        String id,
        String room,
        String author,
        String content,
        Instant sentAt,
        List<String> users,
        Integer onlineCount,
        Boolean typing,
        String message
) {

    public static ChatOutboundEvent message(ChatMessageResponse response) {
        return new ChatOutboundEvent(
                ChatEventType.MESSAGE,
                response.id(),
                response.room(),
                response.author(),
                response.content(),
                response.sentAt(),
                null,
                null,
                null,
                null
        );
    }

    public static ChatOutboundEvent presence(String room, List<String> users) {
        return new ChatOutboundEvent(
                ChatEventType.PRESENCE,
                null,
                room,
                null,
                null,
                null,
                users,
                users.size(),
                null,
                null
        );
    }

    public static ChatOutboundEvent join(String room, String author) {
        return membership(ChatEventType.JOIN, room, author);
    }

    public static ChatOutboundEvent leave(String room, String author) {
        return membership(ChatEventType.LEAVE, room, author);
    }

    public static ChatOutboundEvent typing(String room, String author, boolean typing) {
        return new ChatOutboundEvent(
                ChatEventType.TYPING,
                null,
                room,
                author,
                null,
                null,
                null,
                null,
                typing,
                null
        );
    }

    public static ChatOutboundEvent error(String room, String message) {
        return new ChatOutboundEvent(
                ChatEventType.ERROR,
                null,
                room,
                null,
                null,
                null,
                null,
                null,
                null,
                message
        );
    }

    private static ChatOutboundEvent membership(ChatEventType type, String room, String author) {
        return new ChatOutboundEvent(
                type,
                null,
                room,
                author,
                null,
                Instant.now(),
                null,
                null,
                null,
                null
        );
    }
}
