package com.utilsbot.repository.listeners;

import com.utilsbot.domain.ChatConfig;
import com.utilsbot.domain.Notification;
import com.utilsbot.domain.UserData;
import org.hibernate.HibernateException;
import org.hibernate.event.internal.DefaultDeleteEventListener;
import org.hibernate.event.spi.DeleteEvent;
import org.springframework.stereotype.Component;

@Component
public class DeleteListener extends DefaultDeleteEventListener {

    @Override
    public void onDelete(DeleteEvent event) throws HibernateException {
        super.onDelete(event);

        Object entity = event.getObject();
        if (entity instanceof Notification notification) {
            ChatConfig chatConfig = notification.getChatConfig();
            chatConfig.getNotifications().remove(notification);
        }
        else if (entity instanceof UserData userData) {
            ChatConfig chatConfig = userData.getChatConfig();
            chatConfig.getUserData().remove(userData);
        }
    }
}
