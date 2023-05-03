package com.utilsbot.domain;

import jakarta.persistence.*;
import net.suuft.libretranslate.Language;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "chat_config")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ChatConfig implements Serializable {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "translation_target_lang")
    private Language translationTargetLang;

    @Column(name = "dad_bot")
    private Boolean dadBot;

    public ChatConfig() {
    }

    public ChatConfig(Long chatId) {
        this.chatId = chatId;
        this.translationTargetLang = Language.NONE;
        this.dadBot = false;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Language getTranslationTargetLang() {
        return translationTargetLang;
    }

    public void setTranslationTargetLang(Language translationTargetLang) {
        this.translationTargetLang = translationTargetLang;
    }

    public Boolean getDadBot() {
        return dadBot;
    }

    public void setDadBot(Boolean dadBot) {
        this.dadBot = dadBot;
    }

    public void toggleDadBot() {
        this.dadBot = !dadBot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatConfig that = (ChatConfig) o;
        return chatId.equals(that.chatId) && translationTargetLang == that.translationTargetLang && dadBot.equals(that.dadBot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, translationTargetLang, dadBot);
    }

    @Override
    public String toString() {
        return "ChatConfig{" +
                "chatId=" + chatId +
                ", translationTargetLang=" + translationTargetLang +
                ", dadBot=" + dadBot +
                '}';
    }
}
