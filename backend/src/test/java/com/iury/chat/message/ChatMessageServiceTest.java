package com.iury.chat.message;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMessageServiceTest {

    private final ChatMessageRepository repository = mock(ChatMessageRepository.class);
    private final ChatMessageService service = new ChatMessageService(repository);

    @Test
    void savesNormalizedAndTrimmedMessage() {
        when(repository.save(any(ChatMessage.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.save("  Sala-A  ", new ChatMessageRequest("  Iury  ", "  Ola  ")))
                .assertNext(response -> {
                    assertThat(response.room()).isEqualTo("sala-a");
                    assertThat(response.author()).isEqualTo("Iury");
                    assertThat(response.content()).isEqualTo("Ola");
                    assertThat(response.sentAt()).isNotNull();
                })
                .verifyComplete();

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRoom()).isEqualTo("sala-a");
    }

    @Test
    void rejectsBlankContentBeforePersisting() {
        StepVerifier.create(service.save("geral", new ChatMessageRequest("Iury", "   ")))
                .expectErrorMessage("Escreva uma mensagem antes de enviar")
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void loadsRecentMessagesInChronologicalOrder() {
        ChatMessage newest = new ChatMessage("geral", "Ana", "segunda", Instant.parse("2026-05-21T10:02:00Z"));
        ChatMessage oldest = new ChatMessage("geral", "Iury", "primeira", Instant.parse("2026-05-21T10:01:00Z"));
        when(repository.findTop50ByRoomOrderBySentAtDesc("geral")).thenReturn(Flux.just(newest, oldest));

        StepVerifier.create(service.findRecentByRoom("  Geral  "))
                .expectNextMatches(response -> response.content().equals("primeira"))
                .expectNextMatches(response -> response.content().equals("segunda"))
                .verifyComplete();
    }

    @Test
    void normalizesEmptyRoomToDefaultRoom() {
        assertThat(service.normalizeRoom(null)).isEqualTo("geral");
        assertThat(service.normalizeRoom("   ")).isEqualTo("geral");
        assertThat(service.normalizeRoom(" Sala X ")).isEqualTo("sala x");
    }
}
