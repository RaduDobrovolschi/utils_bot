package com.utilsbot.service;

import com.utilsbot.service.dto.ExpectingInputDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class ExpectingInputService {

    private final HashMap<Long, ExpectingInputDTO> expectingInputMap = new HashMap<>();

    public void addExpectingInput(ExpectingInputDTO expectingInputDto) {
        expectingInputMap.put(expectingInputDto.chatId(), expectingInputDto);
    }

    public void removeExpectingInput(Long key) {
        expectingInputMap.remove(key);
    }

    public ExpectingInputDTO getExpectingInput(Long chatId) {
        return expectingInputMap.get(chatId);
    }

    public boolean hasExpectingInput(Long chatId) {
        return expectingInputMap.get(chatId) != null;
    }

    @Async
    @Scheduled(cron = "${cron.expecting-input-cleanup}")
    public void cleanExpectingInput() {
        Iterator<Map.Entry<Long, ExpectingInputDTO>> iterator = expectingInputMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Instant time = iterator.next().getValue().createTime();
            if (time.isBefore(time.minus(Duration.ofMinutes(5)))) {
                iterator.remove();
            }
        }
    }
}
