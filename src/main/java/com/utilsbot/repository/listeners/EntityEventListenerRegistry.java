package com.utilsbot.repository.listeners;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.stereotype.Component;

@Component
public class EntityEventListenerRegistry {

    @PersistenceUnit
    private EntityManagerFactory emf;

    private final DeleteListener deleteListener;
    private final InsertListener insertListener;
    private final UpdateListener updateListener;

    public EntityEventListenerRegistry(DeleteListener deleteListener,
                                       InsertListener insertListener,
                                       UpdateListener updateListener) {
        this.deleteListener = deleteListener;
        this.insertListener = insertListener;
        this.updateListener = updateListener;
    }

    @PostConstruct
    public void registerListeners() {
        SessionFactoryImpl sessionFactory = emf.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.DELETE).appendListener(deleteListener);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(insertListener);
        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(updateListener);
    }
}