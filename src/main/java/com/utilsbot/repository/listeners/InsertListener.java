package com.utilsbot.repository.listeners;

import com.utilsbot.config.CacheFactoryConfiguration;
import com.utilsbot.domain.Notification;
import com.utilsbot.domain.UserData;
import com.utilsbot.service.NotificationSchedulerService;
import com.utilsbot.service.dto.NotificationToScheduleDto;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class InsertListener implements PostInsertEventListener {

    private final CacheFactoryConfiguration cacheFactoryConfiguration;
    private final NotificationSchedulerService notificationSchedulerService;

    public InsertListener(CacheFactoryConfiguration cacheFactoryConfiguration,
                          NotificationSchedulerService notificationSchedulerService) {
        this.cacheFactoryConfiguration = cacheFactoryConfiguration;
        this.notificationSchedulerService = notificationSchedulerService;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) throws HibernateException {
        Object entity = event.getEntity();

        if (entity instanceof Notification notification && notification.getChatConfig() != null) {
            notification.getChatConfig().getNotifications().add(notification);

            if (LocalDate.from(notification.getScheduledFor()).isEqual(LocalDate.now())) {
                notificationSchedulerService.addNotification(
                        new NotificationToScheduleDto(notification.getId(), notification.getScheduledFor().toInstant(ZoneOffset.UTC))
                );
            }
        }
        else if (entity instanceof UserData userData && userData.getChatConfig() != null) {
            cacheFactoryConfiguration.getCacheManager().getCache("userData-list").clear();
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister entityPersister) {
        return false;
    }
}
