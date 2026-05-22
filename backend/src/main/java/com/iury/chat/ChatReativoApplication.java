package com.iury.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChatReativoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatReativoApplication.class, args);
    }
}
