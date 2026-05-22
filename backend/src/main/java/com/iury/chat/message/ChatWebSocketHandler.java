package com.iury.chat.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iury.chat.presence.PresenceService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatMessageService service;
    private final PresenceService presenceService;
    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.Many<ChatOutboundEvent>> rooms = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(
            ChatMessageService service,
            PresenceService presenceService,
            ObjectMapper objectMapper
    ) {
        this.service = service;
        this.presenceService = presenceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String room = service.normalizeRoom(queryParam(session, "room"));
        String author = service.normalizeAuthor(queryParam(session, "author"));
        String sessionId = session.getId();

        Sinks.Many<ChatOutboundEvent> roomSink = rooms.computeIfAbsent(
                room,
                key -> Sinks.many().multicast().directBestEffort()
        );
        Sinks.Many<ChatOutboundEvent> privateSink = Sinks.many().unicast().onBackpressureBuffer();

        Flux<WebSocketMessage> outgoing = Flux.merge(roomSink.asFlux(), privateSink.asFlux())
                .map(event -> session.textMessage(writeResponse(event)));

        Mono<Void> register = presenceService.join(room, sessionId, author)
                .doOnNext(users -> emitPresence(roomSink, room, users))
                .doOnSuccess(users -> roomSink.tryEmitNext(ChatOutboundEvent.join(room, author)))
                .then();

        Mono<Void> receive = register.thenMany(session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .flatMap(payload -> handlePayload(payload, room, sessionId, author, roomSink, privateSink)))
                .then();

        Mono<Void> socket = session.send(outgoing).and(receive);

        return socket
                .doFinally(signal -> presenceService.leave(room, sessionId)
                        .doOnNext(users -> emitPresence(roomSink, room, users))
                        .doOnSuccess(users -> roomSink.tryEmitNext(ChatOutboundEvent.leave(room, author)))
                        .subscribe());
    }

    private Mono<Void> handlePayload(
            String payload,
            String room,
            String sessionId,
            String sessionAuthor,
            Sinks.Many<ChatOutboundEvent> roomSink,
            Sinks.Many<ChatOutboundEvent> privateSink
    ) {
        ChatInboundEvent event;
        try {
            event = readRequest(payload);
        } catch (IllegalArgumentException exception) {
            privateSink.tryEmitNext(ChatOutboundEvent.error(room, exception.getMessage()));
            return Mono.empty();
        }

        ChatEventType type = event.type() == null ? ChatEventType.MESSAGE : event.type();
        String author = service.normalizeAuthor(event.author() == null ? sessionAuthor : event.author());

        return presenceService.refresh(room, sessionId, author)
                .flatMap(users -> {
                    Mono<Void> action = switch (type) {
                        case MESSAGE -> service.save(room, new ChatMessageRequest(author, event.content()))
                                .doOnNext(response -> roomSink.tryEmitNext(ChatOutboundEvent.message(response)))
                                .then(Mono.fromRunnable(() -> emitPresence(roomSink, room, users)));
                        case TYPING -> Mono.fromRunnable(() -> roomSink.tryEmitNext(
                                ChatOutboundEvent.typing(room, author, Boolean.TRUE.equals(event.typing()))
                        ));
                        default -> Mono.fromRunnable(() -> privateSink.tryEmitNext(
                                ChatOutboundEvent.error(room, "Evento WebSocket nao suportado pelo cliente")
                        ));
                    };
                    return action;
                })
                .onErrorResume(exception -> {
                    privateSink.tryEmitNext(ChatOutboundEvent.error(room, exception.getMessage()));
                    return Mono.empty();
                });
    }

    private ChatInboundEvent readRequest(String payload) {
        try {
            return objectMapper.readValue(payload, ChatInboundEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Payload WebSocket invalido", exception);
        }
    }

    private void emitPresence(Sinks.Many<ChatOutboundEvent> sink, String room, List<?> users) {
        List<String> authors = users.stream()
                .map(user -> user instanceof com.iury.chat.presence.PresenceUser presenceUser
                        ? presenceUser.author()
                        : String.valueOf(user))
                .distinct()
                .sorted()
                .toList();
        sink.tryEmitNext(ChatOutboundEvent.presence(room, authors));
    }

    private String queryParam(WebSocketSession session, String name) {
        return UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams()
                .getFirst(name);
    }

    private String writeResponse(ChatOutboundEvent response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel serializar o evento", exception);
        }
    }
}
