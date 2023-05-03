package com.utilsbot.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

//on hold for now
//@Service
public class NotificationService {
    private final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final TaskScheduler taskScheduler;

    public NotificationService(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    private void postConstruct() {
        //taskScheduler.schedule();
    }

    public void scheduleNotification() {

    }
}
