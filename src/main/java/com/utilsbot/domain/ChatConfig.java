package com.utilsbot.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import net.suuft.libretranslate.Language;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.utilsbot.utils.TimeUtils.getOffsetDate;
import static com.utilsbot.utils.TimeUtils.getOffsetDateTime;

@Entity
@Table(name = "chat_config")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ChatConfig implements Serializable {

    @Id
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "translation_target_lang")
    private Language translationTargetLang;

    @Column(name = "dad_bot")
    private Boolean dadBot;

    @Column(name = "gmt_offset")
    private Float gmtOffset;

    @BatchSize(size = 25)
    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(mappedBy = "chatConfig", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "userData-list")
    private Set<UserData> userData = new HashSet<>();

    @BatchSize(size = 5)
    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(mappedBy = "chatConfig", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "notification-list")
    private Set<Notification> notifications = new HashSet<>();

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
        return getOffsetDateTime(gmtOffset);
    }

    public LocalDate getUserDate() {
        return getOffsetDate(gmtOffset);
    }

    public boolean hasGtmOffset() {
        return this.gmtOffset != null;
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

    public Set<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<Notification> notifications) {
        this.notifications = notifications;
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
                "id=" + id +
                ", translationTargetLang=" + translationTargetLang +
                ", dadBot=" + dadBot +
                ", gmtOffset=" + gmtOffset +
                '}';
    }
}
