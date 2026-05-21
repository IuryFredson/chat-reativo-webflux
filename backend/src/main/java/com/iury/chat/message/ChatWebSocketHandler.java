package com.iury.chat.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatMessageService service;
    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.Many<ChatMessageResponse>> rooms = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatMessageService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String room = roomFrom(session);
        Sinks.Many<ChatMessageResponse> sink = rooms.computeIfAbsent(
                room,
                key -> Sinks.many().multicast().directBestEffort()
        );

        Mono<Void> receive = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(this::readRequest)
                .flatMap(request -> service.save(room, request))
                .doOnNext(sink::tryEmitNext)
                .then();

        Mono<Void> send = session.send(
                sink.asFlux().map(message -> session.textMessage(writeResponse(message)))
        );

        return send.and(receive);
    }

    private String roomFrom(WebSocketSession session) {
        String room = UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams()
                .getFirst("room");

        return room == null || room.isBlank() ? "geral" : room.trim().toLowerCase();
    }

    private ChatMessageRequest readRequest(String payload) {
        try {
            return objectMapper.readValue(payload, ChatMessageRequest.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Mensagem WebSocket invalida", exception);
        }
    }

    private String writeResponse(ChatMessageResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel serializar a mensagem", exception);
        }
    }
}
