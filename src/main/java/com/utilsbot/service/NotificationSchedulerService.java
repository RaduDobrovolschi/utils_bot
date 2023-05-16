package com.utilsbot.service;

import com.utilsbot.bots.UtilsBot;
import com.utilsbot.config.CacheFactoryConfiguration;
import com.utilsbot.domain.UserData;
import com.utilsbot.service.dto.NotificationToScheduleDto;
import jakarta.annotation.PostConstruct;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Service
public class NotificationSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchedulerService.class);

    private final UtilsBot utilsBot;
    private final TaskScheduler taskScheduler;
    private final JdbcTemplate jdbcTemplate;
    private final UserDataService userDataService;
    private final CacheFactoryConfiguration cacheFactoryConfiguration;

    public NotificationSchedulerService(UtilsBot utilsBot,
                                        TaskScheduler taskScheduler,
                                        JdbcTemplate jdbcTemplate,
                                        UserDataService userDataService,
                                        CacheFactoryConfiguration cacheFactoryConfiguration) {
        this.utilsBot = utilsBot;
        this.taskScheduler = taskScheduler;
        this.jdbcTemplate = jdbcTemplate;
        this.userDataService = userDataService;
        this.cacheFactoryConfiguration = cacheFactoryConfiguration;
    }

    @PostConstruct
    private void postConstruct() {
        scheduleNotifications();
    }

    @Async
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleNotifications() {
        List<NotificationToScheduleDto> notificationsToSchedule = jdbcTemplate.query(
                """
                        select chat_id, custom_msg_id, scheduled_for from
                            (select chat_id, gmt_offset, custom_msg_id, scheduled_for,
                                    apply_offset(scheduled_for, gmt_offset) as zoned_date
                                from chat_config c
                                    inner join notification n on n.chat_id = c.id
                                    where gmt_offset is not null
                            ) as cn
                        where zoned_date::date = apply_offset(current_timestamp::timestamp without time zone, gmt_offset)::date;
                        """, (rs, rowNum) -> new NotificationToScheduleDto(rs.getLong(1), rs.getLong(2), rs.getTimestamp(3).toInstant())
        );
        log.info("scheduling {} notifications...", notificationsToSchedule.size());
        notificationsToSchedule.forEach(this::addNotification);
    }

    public void addNotification(NotificationToScheduleDto notification) {
        taskScheduler.schedule(() -> {
            log.info("running scheduled notification {}", notification);
            SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                    .chatId(notification.chatId());
            Set<UserData> allUserData = userDataService.getAllUserData(notification.chatId());
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
            } catch (TelegramApiException e) {
                log.error("failed to send scheduled notification {}", notification);
                e.printStackTrace();
            }

            //todo mb replace with delete through repository to avoid cleaning cache
            jdbcTemplate.update("delete from notification where chat_id = ? and scheduled_for = ?",
                                    notification.chatId(), Timestamp.from(notification.triggerTime()));
            EmbeddedCacheManager cacheManager = cacheFactoryConfiguration.getCacheManager();
            cacheManager.getCache("notification").clear();
            cacheManager.getCache("notification-list").clear();
        }, notification.triggerTime());
    }
}
