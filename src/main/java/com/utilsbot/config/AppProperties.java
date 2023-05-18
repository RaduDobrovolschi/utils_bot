package com.utilsbot.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public class AppProperties {

    private String translationApiUrl;

    @NotEmpty
    private String ocrApiKey;

    @NotEmpty
    private String timezoneApiKey;

    private final Bot bot = new Bot();

    private final Microservices microservices = new Microservices();

    public Bot getBot() {
        return bot;
    }

    public Microservices getMicroservices() {
        return microservices;
    }

    public String getOcrApiKey() {
        return ocrApiKey;
    }

    public void setOcrApiKey(String ocrApiKey) {
        this.ocrApiKey = ocrApiKey;
    }

    public String getTimezoneApiKey() {
        return timezoneApiKey;
    }

    public void setTimezoneApiKey(String timezoneApiKey) {
        this.timezoneApiKey = timezoneApiKey;
    }

    public String getTranslationApiUrl() {
        return translationApiUrl;
    }

    public void setTranslationApiUrl(String translationApiUrl) {
        this.translationApiUrl = translationApiUrl;
    }

    public static class Bot {
        @NotEmpty
        private String token;

        @NotEmpty
        private String username;

        @NotEmpty
        private int notificationsLimit;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getNotificationsLimit() {
            return notificationsLimit;
        }

        public void setNotificationsLimit(int notificationsLimit) {
            this.notificationsLimit = notificationsLimit;
        }
    }

    public static class Microservices {
        @NotEmpty
        private String whisperUrl;

        public String getWhisperUrl() {
            return whisperUrl;
        }

        public void setWhisperUrl(String whisperUrl) {
            this.whisperUrl = whisperUrl;
        }
    }
}