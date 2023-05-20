package com.utilsbot.service;

import com.utilsbot.bots.UtilsBot;
import com.utilsbot.domain.UserData;
import com.utilsbot.repository.NotificationRepository;
import com.utilsbot.service.dto.NotificationToScheduleDTO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Set;

@Service
public class NotificationSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchedulerService.class);

    private final UtilsBot utilsBot;
    private final TaskScheduler taskScheduler;
    private final JdbcTemplate jdbcTemplate;
    private final UserDataService userDataService;
    private final NotificationRepository notificationRepository;

    public NotificationSchedulerService(UtilsBot utilsBot,
                                        TaskScheduler taskScheduler,
                                        JdbcTemplate jdbcTemplate,
                                        UserDataService userDataService,
                                        NotificationRepository notificationRepository) {
        this.utilsBot = utilsBot;
        this.taskScheduler = taskScheduler;
        this.jdbcTemplate = jdbcTemplate;
        this.userDataService = userDataService;
        this.notificationRepository = notificationRepository;
    }

    @PostConstruct
    private void postConstruct() {
        scheduleNotifications();
    }

    @Async
    @Scheduled(cron = "1 0 0 * * *")
    public void scheduleNotifications() {
        log.info("extracting scheduled notifications");
        List<NotificationToScheduleDTO> notificationsToSchedule = jdbcTemplate.query(
                """
                        select id, scheduled_for
                            from notification
                            where scheduled_for::date = current_date;
                        """, (rs, rowNum) -> new NotificationToScheduleDTO(rs.getLong(1), rs.getTimestamp(2).toInstant())
        );
        log.info("scheduling {} notifications...", notificationsToSchedule.size());
        notificationsToSchedule.forEach(this::addNotification);
    }

    public void addNotification(NotificationToScheduleDTO notificationDto) {
        taskScheduler.schedule(() ->
                notificationRepository.findById(notificationDto.notificationId()).ifPresent(notification -> {
                log.info("running scheduled notification {}", notification);
                    Long chatId = notification.getChatConfig().getId();
                    SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                        .chatId(chatId);
                Set<UserData> allUserData = userDataService.getAllUserData(chatId);
                String msg = "Running scheduled notification ";
                if (!allUserData.isEmpty()) {
                    utilsBot.addUserMentions(sendMessageBuilder,
                            msg.length(),
                            names -> names.insert(0, msg).toString(),
                            allUserData
                    );
                } else {
                    sendMessageBuilder.text(msg);
                }
                try {
                    utilsBot.executeAsync(sendMessageBuilder.build());
                    utilsBot.executeAsync(
                            CopyMessage.builder()
                                    .fromChatId(chatId)
                                    .chatId(chatId)
                                    .messageId(notification.getCustomMsgId())
                                    .build()
                    );
                } catch (TelegramApiException e) {
                log.error("failed to send scheduled notification {}", notification);
                e.printStackTrace();
                }
                notificationRepository.delete(notification);
        }), notificationDto.triggerTime());
    }
}
