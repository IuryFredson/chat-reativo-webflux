package com.iury.chat.message;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class ChatMessageService {

    private final ChatMessageRepository repository;

    public ChatMessageService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    public Mono<ChatMessageResponse> save(String room, ChatMessageRequest request) {
        ChatMessage message = new ChatMessage(
                normalize(room),
                request.author().trim(),
                request.content().trim(),
                Instant.now()
        );

        return repository.save(message).map(ChatMessageResponse::from);
    }

    public Flux<ChatMessageResponse> findRecentByRoom(String room) {
        return repository.findTop50ByRoomOrderBySentAtDesc(normalize(room))
                .sort((left, right) -> left.getSentAt().compareTo(right.getSentAt()))
                .map(ChatMessageResponse::from);
    }

    private String normalize(String room) {
        return room == null || room.isBlank() ? "geral" : room.trim().toLowerCase();
    }
}
