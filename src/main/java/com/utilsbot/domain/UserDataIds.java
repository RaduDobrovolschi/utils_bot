package com.utilsbot.domain;

import java.io.Serializable;
import java.util.Objects;

public class UserDataIds implements Serializable {
    private Long userId;
    private ChatConfig chatConfig;

    public UserDataIds() {
    }

    public UserDataIds(Long userId, ChatConfig chatConfig) {
        this.userId = userId;
        this.chatConfig = chatConfig;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ChatConfig getChatConfig() {
        return chatConfig;
    }

    public void setChatConfig(ChatConfig chatConfig) {
        this.chatConfig = chatConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDataIds that = (UserDataIds) o;
        return userId.equals(that.userId) && chatConfig.getId().equals(that.chatConfig.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, chatConfig.getId());
    }

    @Override
    public String toString() {
        return "UserDataIds{" +
                "userId=" + userId +
                ", chatConfig=" + chatConfig +
                '}';
    }
}
