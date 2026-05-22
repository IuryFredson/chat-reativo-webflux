package com.iury.chat.presence;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.presence")
public record PresenceProperties(Duration ttl) {

    public PresenceProperties {
        if (ttl == null) {
            ttl = Duration.ofSeconds(45);
        }
    }
}
