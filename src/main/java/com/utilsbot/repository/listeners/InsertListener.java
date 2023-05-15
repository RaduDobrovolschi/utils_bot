package com.utilsbot.repository.listeners;

import com.utilsbot.config.CacheFactoryConfiguration;
import com.utilsbot.domain.Notification;
import com.utilsbot.domain.UserData;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

@Component
public class InsertListener implements PostInsertEventListener {

    private final CacheFactoryConfiguration cacheFactoryConfiguration;

    public InsertListener(CacheFactoryConfiguration cacheFactoryConfiguration) {
        this.cacheFactoryConfiguration = cacheFactoryConfiguration;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) throws HibernateException {
        Object entity = event.getEntity();

        if (entity instanceof Notification notification && notification.getChatConfig() != null) {
            notification.getChatConfig().getNotifications().add(notification);
        }

        if (entity instanceof UserData userData && userData.getChatConfig() != null) {
            cacheFactoryConfiguration.getCacheManager().getCache("userData-list").clear();
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister entityPersister) {
        return false;
    }
}
