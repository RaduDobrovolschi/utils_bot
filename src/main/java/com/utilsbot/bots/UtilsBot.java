package com.utilsbot.bots;

import com.utilsbot.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class UtilsBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(UtilsBot.class);

    private final AppProperties appProperties;

    public UtilsBot(AppProperties appProperties) {
        super(appProperties.getBot().getToken());
        this.appProperties = appProperties;
    }

    @PostConstruct
    private void postConstruct() {
        log.info("setting bot commands");
        try {
            executeAsync(
                    SetMyCommands.builder()
                            .command(new BotCommand("/info", "info"))
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error("failed to set bot commands");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {

    }

    @Override
    public String getBotUsername() {
        return appProperties.getBot().getUsername();
    }
}