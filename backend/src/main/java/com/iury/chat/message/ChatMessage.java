package com.iury.chat.message;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("messages")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String room;

    private String author;
    private String content;
    private Instant sentAt;

    public ChatMessage() {
    }

    public ChatMessage(String room, String author, String content, Instant sentAt) {
        this.room = room;
        this.author = author;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getId() {
        return id;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
