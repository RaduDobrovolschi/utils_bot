package com.utilsbot.repository.listeners;

import com.utilsbot.config.CacheFactoryConfiguration;
import com.utilsbot.domain.Notification;
import com.utilsbot.domain.UserData;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

@Component
public class UpdateListener implements PostUpdateEventListener {

    private final CacheFactoryConfiguration cacheFactoryConfiguration;

    public UpdateListener(CacheFactoryConfiguration cacheFactoryConfiguration) {
        this.cacheFactoryConfiguration = cacheFactoryConfiguration;
    }

    @Override
    public void onPostUpdate(PostUpdateEvent postUpdateEvent) {
        Object entity = postUpdateEvent.getEntity();

        if (entity instanceof Notification notification && notification.getChatConfig() != null) {
            cacheFactoryConfiguration.getCacheManager().getCache("notification-list").clear();
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
