package com.iury.chat.message;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ChatMessageRepository extends ReactiveMongoRepository<ChatMessage, String> {

    Flux<ChatMessage> findTop50ByRoomOrderBySentAtDesc(String room);
}
