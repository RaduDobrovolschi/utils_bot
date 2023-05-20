package com.utilsbot.service;

import com.utilsbot.domain.ChatConfig;
import com.utilsbot.domain.Notification;
import com.utilsbot.repository.NotificationRepository;
import com.utilsbot.service.dto.ResponseMsgDataDTO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.utilsbot.domain.enums.MessagesEnum.NOTIFICATION_UPDATE;
import static com.utilsbot.keyboard.CustomKeyboards.getKeyboard;
import static com.utilsbot.utils.TimeUtils.defaultDTFormatter;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ChatConfigService chatConfigService;

    public NotificationService(NotificationRepository notificationRepository,
                               ChatConfigService chatConfigService) {
        this.notificationRepository = notificationRepository;
        this.chatConfigService = chatConfigService;
    }

    public void addNotification(LocalDateTime inputTime, Long chatId) {
        ChatConfig chatConfig = chatConfigService.getChatConfig(chatId);
        notificationRepository.save(
                new Notification(
                        inputTime,
                        chatConfig
                ));
    }

    public ResponseMsgDataDTO createEditNotificationMsg(Long nId) {
        Optional<Notification> byId = notificationRepository.findById(nId);
        if (byId.isPresent()) {
            Notification notification = byId.get();
            Duration between = Duration.between(LocalDateTime.now(), notification.getScheduledFor());
            return new ResponseMsgDataDTO(
                    String.format(NOTIFICATION_UPDATE.getValue(),
                            notification.getZonedScheduledFor().format(defaultDTFormatter),
                            between.toHours(), between.toMinutes() % 60),
                    getKeyboard(NOTIFICATION_UPDATE)
            );
        }
        return null;
    }

    public void setCustomMsg(Long nfId, Integer msgId) {
        notificationRepository.findById(nfId).ifPresent(notification -> notification.setCustomMsgId(msgId));
    }

    public void delete(Long nId) {
        notificationRepository.deleteById(nId);
    }
}
