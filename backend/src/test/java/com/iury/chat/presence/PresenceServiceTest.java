package com.iury.chat.presence;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PresenceServiceTest {

    private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
    private final ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
    private final PresenceService service = new PresenceService(redis, new PresenceProperties(Duration.ofSeconds(30)));

    @Test
    void refreshesSessionWithTtlAndReturnsActiveUsers() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.set("chat:presence:geral:s1", "Iury", Duration.ofSeconds(30))).thenReturn(Mono.just(true));
        when(redis.keys("chat:presence:geral:*")).thenReturn(Flux.just(
                "chat:presence:geral:s2",
                "chat:presence:geral:s1"
        ));
        when(values.get("chat:presence:geral:s1")).thenReturn(Mono.just("Iury"));
        when(values.get("chat:presence:geral:s2")).thenReturn(Mono.just("Ana"));

        StepVerifier.create(service.refresh("geral", "s1", "Iury"))
                .assertNext(users -> {
                    assertThat(users).extracting(PresenceUser::author).containsExactly("Ana", "Iury");
                    assertThat(users).extracting(PresenceUser::sessionId).containsExactly("s2", "s1");
                })
                .verifyComplete();

        verify(values).set("chat:presence:geral:s1", "Iury", Duration.ofSeconds(30));
    }

    @Test
    void removesSessionOnLeaveAndReturnsRemainingUsers() {
        when(redis.opsForValue()).thenReturn(values);
        when(redis.delete("chat:presence:geral:s1")).thenReturn(Mono.just(1L));
        when(redis.keys("chat:presence:geral:*")).thenReturn(Flux.just("chat:presence:geral:s2"));
        when(values.get("chat:presence:geral:s2")).thenReturn(Mono.just("Ana"));

        StepVerifier.create(service.leave("geral", "s1"))
                .assertNext(users -> assertThat(users).extracting(PresenceUser::author).containsExactly("Ana"))
                .verifyComplete();
    }

    @Test
    void exposesDistinctActiveAuthors() {
        when(redis.opsForValue()).thenReturn(values);
        when(redis.keys("chat:presence:geral:*")).thenReturn(Flux.just(
                "chat:presence:geral:s1",
                "chat:presence:geral:s2",
                "chat:presence:geral:s3"
        ));
        when(values.get("chat:presence:geral:s1")).thenReturn(Mono.just("Iury"));
        when(values.get("chat:presence:geral:s2")).thenReturn(Mono.just("Iury"));
        when(values.get("chat:presence:geral:s3")).thenReturn(Mono.just("Ana"));

        StepVerifier.create(service.activeAuthors("geral"))
                .expectNext("Ana")
                .expectNext("Iury")
                .verifyComplete();
    }
}
