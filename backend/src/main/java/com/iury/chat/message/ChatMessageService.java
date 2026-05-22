package com.iury.chat.message;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class ChatMessageService {

    private static final int AUTHOR_LIMIT = 40;
    private static final int CONTENT_LIMIT = 500;

    private final ChatMessageRepository repository;

    public ChatMessageService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    public Mono<ChatMessageResponse> save(String room, ChatMessageRequest request) {
        return validate(request).flatMap(validRequest -> {
            ChatMessage message = new ChatMessage(
                    normalizeRoom(room),
                    validRequest.author().trim(),
                    validRequest.content().trim(),
                    Instant.now()
            );

            return repository.save(message).map(ChatMessageResponse::from);
        });
    }

    public Flux<ChatMessageResponse> findRecentByRoom(String room) {
        return repository.findTop50ByRoomOrderBySentAtDesc(normalizeRoom(room))
                .sort((left, right) -> left.getSentAt().compareTo(right.getSentAt()))
                .map(ChatMessageResponse::from);
    }

    public String normalizeRoom(String room) {
        return room == null || room.isBlank() ? "geral" : room.trim().toLowerCase();
    }

    public String normalizeAuthor(String author) {
        if (author == null || author.isBlank()) {
            return "anonimo";
        }
        String normalized = author.trim();
        return normalized.length() > AUTHOR_LIMIT ? normalized.substring(0, AUTHOR_LIMIT) : normalized;
    }

    private Mono<ChatMessageRequest> validate(ChatMessageRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Payload de mensagem vazio"));
        }
        if (request.author() == null || request.author().isBlank()) {
            return Mono.error(new IllegalArgumentException("Informe o nome antes de enviar"));
        }
        if (request.content() == null || request.content().isBlank()) {
            return Mono.error(new IllegalArgumentException("Escreva uma mensagem antes de enviar"));
        }
        if (request.author().trim().length() > AUTHOR_LIMIT) {
            return Mono.error(new IllegalArgumentException("Nome deve ter no maximo 40 caracteres"));
        }
        if (request.content().trim().length() > CONTENT_LIMIT) {
            return Mono.error(new IllegalArgumentException("Mensagem deve ter no maximo 500 caracteres"));
        }
        return Mono.just(request);
    }
}
