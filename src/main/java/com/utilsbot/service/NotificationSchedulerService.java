package com.utilsbot.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

//@Service
public class NotificationSchedulerService {

    private final TaskScheduler taskScheduler;
    private final JdbcTemplate jdbcTemplate;

    public NotificationSchedulerService(TaskScheduler taskScheduler,
                                        JdbcTemplate jdbcTemplate) {
        this.taskScheduler = taskScheduler;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    private void postConstruct() {
        //taskScheduler.schedule();

//              """
//                select chat_id, custom_msg_id, scheduled_for from
//                    (select chat_id, gmt_offset, custom_msg_id, scheduled_for,
//                            apply_offset(scheduled_for, gmt_offset) as zoned_date
//                        from chat_config c
//                            inner join notification n on n.chat_id = c.id
//                            where gmt_offset is not null
//                    ) as cn
//                where zoned_date::date = apply_offset(current_timestamp::timestamp without time zone, 5.3)::date;
//                """
    }
}
