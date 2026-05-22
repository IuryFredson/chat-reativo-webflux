package com.iury.chat.presence;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Service
public class PresenceService {

    private static final String KEY_PREFIX = "chat:presence:";

    private final ReactiveStringRedisTemplate redis;
    private final PresenceProperties properties;

    public PresenceService(ReactiveStringRedisTemplate redis, PresenceProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public Mono<List<PresenceUser>> join(String room, String sessionId, String author) {
        return refresh(room, sessionId, author);
    }

    public Mono<List<PresenceUser>> refresh(String room, String sessionId, String author) {
        return redis.opsForValue()
                .set(key(room, sessionId), author, properties.ttl())
                .then(activeUsers(room));
    }

    public Mono<List<PresenceUser>> leave(String room, String sessionId) {
        return redis.delete(key(room, sessionId)).then(activeUsers(room));
    }

    public Mono<List<PresenceUser>> activeUsers(String room) {
        return redis.keys(KEY_PREFIX + room + ":*")
                .flatMap(key -> redis.opsForValue()
                        .get(key)
                        .map(author -> new PresenceUser(sessionIdFrom(key), author)))
                .sort(Comparator.comparing(PresenceUser::author).thenComparing(PresenceUser::sessionId))
                .collectList();
    }

    public Flux<String> activeAuthors(String room) {
        return activeUsers(room)
                .flatMapMany(Flux::fromIterable)
                .map(PresenceUser::author)
                .distinct();
    }

    private String key(String room, String sessionId) {
        return KEY_PREFIX + room + ":" + sessionId;
    }

    private String sessionIdFrom(String key) {
        int index = key.lastIndexOf(':');
        return index >= 0 ? key.substring(index + 1) : key;
    }
}
