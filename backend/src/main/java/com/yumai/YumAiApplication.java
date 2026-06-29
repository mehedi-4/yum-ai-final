package com.yumai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * YumAI - AI-Powered Restaurant Management SaaS.
 * Entry point for the Spring Boot backend.
 */
@SpringBootApplication
@EnableScheduling
public class YumAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(YumAiApplication.class, args);
    }
}
