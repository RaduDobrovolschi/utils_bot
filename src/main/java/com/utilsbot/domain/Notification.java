package com.utilsbot.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.utilsbot.utils.TimeUtils.applyOffset;

@Entity
@Table(name = "notification")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "notification")
public class Notification implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;

    @Column(name = "custom_msg_id")
    private Integer customMsgId;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @NotNull
    @Fetch(FetchMode.SELECT)
    @ManyToOne(optional = false)
    @JoinColumn(name="chat_id", nullable = false)
    private ChatConfig chatConfig;

    public Notification() {
    }

    public Notification(Integer customMsgId, LocalDateTime scheduledFor, ChatConfig chatConfig) {
        this.customMsgId = customMsgId;
        this.scheduledFor = scheduledFor;
        this.chatConfig = chatConfig;
    }

    public Notification(LocalDateTime scheduledFor, ChatConfig chatConfig) {
        this.scheduledFor = scheduledFor;
        this.chatConfig = chatConfig;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCustomMsgId() {
        return customMsgId;
    }

    public void setCustomMsgId(Integer customMsgId) {
        this.customMsgId = customMsgId;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public LocalDateTime getZonedScheduledFor() {
        return applyOffset(chatConfig.getGmtOffset(), scheduledFor);
    }

    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
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
        Notification that = (Notification) o;
        return id.equals(that.id) && Objects.equals(customMsgId, that.customMsgId) && scheduledFor.equals(that.scheduledFor) && chatConfig.equals(that.chatConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customMsgId, scheduledFor, chatConfig);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", customMsgId=" + customMsgId +
                ", scheduledFor=" + scheduledFor +
                ", chatConfig=" + chatConfig +
                '}';
    }
}
