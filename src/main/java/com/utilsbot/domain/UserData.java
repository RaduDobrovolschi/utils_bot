package com.utilsbot.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Objects;

@Entity
@Table(name = "user_data")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@IdClass(UserDataIds.class)
public class UserData {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @NotNull
    @Fetch(FetchMode.SELECT)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="chat_id", nullable = false)
    private ChatConfig chatConfig;

    public UserData() {
    }

    public UserData(Long userId, ChatConfig chatConfig) {
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
        UserData userData = (UserData) o;
        return userId.equals(userData.userId) && chatConfig.equals(userData.chatConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, chatConfig);
    }

    @Override
    public String toString() {
        return "UserData{" +
                "userId=" + userId +
                ", chatConfig=" + chatConfig +
                '}';
    }
}
