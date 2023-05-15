package com.utilsbot.service;

import com.utilsbot.domain.ChatConfig;
import com.utilsbot.domain.Notification;
import com.utilsbot.repository.NotificationRepository;
import com.utilsbot.service.dto.ResponseMsgDataDTO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.utilsbot.config.TimeConfig.defaultDTFormatter;
import static com.utilsbot.domain.enums.MessagesEnum.NOTIFICATION_UPDATE;
import static com.utilsbot.keyboard.CustomKeyboards.getKeyboard;

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
        Notification save = notificationRepository.save(
                new Notification(
                        inputTime,
                        chatConfig
                ));
//        chatConfig.getNotifications().add(save);
    }

    //todo add time till notification to msg
    public ResponseMsgDataDTO createEditNotificationMsg(Long nId) {
        Optional<Notification> byId = notificationRepository.findById(nId);
        if (byId.isPresent()) {
            Notification notification = byId.get();
            return new ResponseMsgDataDTO(
                    String.format(NOTIFICATION_UPDATE.getValue(), notification.getScheduledFor().format(defaultDTFormatter)),
                    getKeyboard(NOTIFICATION_UPDATE)
            );
        }
        return null;
    }

    public void delete(Long nId) {
        notificationRepository.deleteById(nId);
    }
}
