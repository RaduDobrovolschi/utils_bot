package com.utilsbot.domain;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import net.suuft.libretranslate.Language;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "chat_config")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ChatConfig implements Serializable {

    @Id
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "translation_target_lang")
    private Language translationTargetLang;

    @Column(name = "dad_bot")
    private Boolean dadBot;

    @Column(name = "gmt_offset")
    private Float gmtOffset;

    @BatchSize(size = 25)
    @Fetch(FetchMode.SELECT)
    @OneToMany(mappedBy = "chatConfig", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<UserData> userData = new HashSet<>();

    public ChatConfig() {
    }

    public ChatConfig(Long id) {
        this.id = id;
        this.translationTargetLang = Language.NONE;
        this.dadBot = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long chatId) {
        this.id = chatId;
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

    public Float getGmtOffset() {
        return gmtOffset;
    }

    public LocalDateTime getUserTime() {
        float offset = gmtOffset;
        return OffsetDateTime.now(ZoneOffset.ofHoursMinutes((int)offset, (int)((offset - (int)offset) * 60))).toLocalDateTime();
    }

    public void setGmtOffset(Float gmtOffset) {
        this.gmtOffset = gmtOffset;
    }

    public Set<UserData> getUserData() {
        return userData;
    }

    public void setUserData(Set<UserData> userData) {
        this.userData = userData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatConfig that = (ChatConfig) o;
        return id.equals(that.id) && translationTargetLang == that.translationTargetLang && dadBot.equals(that.dadBot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, translationTargetLang, dadBot);
    }

    @Override
    public String toString() {
        return "ChatConfig{" +
                "chatId=" + id +
                ", translationTargetLang=" + translationTargetLang +
                ", dadBot=" + dadBot +
                '}';
    }
}
