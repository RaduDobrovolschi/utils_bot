package com.utilsbot.bots;

import com.utilsbot.config.AppProperties;
import com.utilsbot.domain.ChatConfig;
import com.utilsbot.domain.enums.CallbackDataEnum;
import com.utilsbot.domain.enums.MessagesEnum;
import com.utilsbot.service.ChatConfigService;
import com.utilsbot.service.OcrService;
import jakarta.annotation.PostConstruct;
import net.suuft.libretranslate.Language;
import net.suuft.libretranslate.Translator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.utilsbot.domain.enums.MessagesEnum.LANG_SELECT_MSG;
import static com.utilsbot.keyboard.CustomKeyboards.*;
import static com.utilsbot.utils.AppUtils.fromCode;

/*
 *
 * todo list of shit that can be added
 *   notifications -> this one is gonna be a pain
 *   ocr -> to add language selection
 *   voice message to text -> reee no free api will have to use a separate microservice
 *   audio file to vm
 *
 */

@Service
public class UtilsBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(UtilsBot.class);

    private final AppProperties appProperties;
    private final ChatConfigService chatConfigService;
    private final OcrService ocrService;
    private final Executor asyncExecutor;

    public UtilsBot(AppProperties appProperties,
                    ChatConfigService chatConfigService,
                    OcrService ocrService,
                    @Qualifier("taskExecutor") Executor asyncExecutor) {
        super(appProperties.getBot().getToken());
        this.appProperties = appProperties;
        this.chatConfigService = chatConfigService;
        this.ocrService = ocrService;
        this.asyncExecutor = asyncExecutor;
    }

    @PostConstruct
    private void postConstruct() {
        log.debug("Setting bot commands");
        try {
            executeAsync(
                    SetMyCommands.builder()
                            .command(new BotCommand("/info", "info"))
                            .command(new BotCommand("/everyone", "ping everyone in a group chat"))
                            .command(new BotCommand("/translate <language code>", "enable translation"))
                            .command(new BotCommand("/translate", "disable translation"))
                            .command(new BotCommand("/DadBot", "enable/disable dad bot"))
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error("failed to set bot commands");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.debug("Update request: {}", update);

        asyncExecutor.execute(() -> { //should consider to async only handlePhoto()
            if (update.hasMessage()) {
                Message message = update.getMessage();

                //ignore messages older than 1 hour
                if ((int)(new Date().getTime()/1000) - message.getDate() > 3600)
                    return;

                if (message.hasText()) {
                    String text = message.getText();
                    if (text.startsWith("/")) {
                        handleCommand(text.toLowerCase(), update);
                    } else {
                        handleTextMessage(text, update);
                    }
                }
                if (message.hasPhoto()) {
                    handlePhoto(message);
                }
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        });
    }

    private void handleCommand(String lowerCaseMessage, Update update) {
        Message message = update.getMessage();
        String responseMsg = null;

        switch (lowerCaseMessage) {
            case "/info", "/start" -> {
                ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                try {
                    executeAsync(
                            SendMessage.builder()
                                    .chatId(message.getChatId())
                                    .text(MessagesEnum.START_MESSAGE.getValue())
                                    .replyMarkup(infoKeyboard(chatConfig.getDadBot(), chatConfig.getTranslationTargetLang()))
                                    .build()
                    );
                } catch (TelegramApiException e) {
                    log.info("failed to send start message update: {}", update);
                    throw new RuntimeException(e);
                }
            }
            case "/everyone" -> {
                if (update.getMessage().getChat().getType().equals("private"))
                    responseMsg = "can't use /everyone in private chat";
                else{
                    Long chatId = update.getMessage().getChat().getId();
                    List<String> chatUsernames;
                    ArrayList<ChatMember> chatAdmins;
                    try {
                        chatAdmins = execute(
                                GetChatAdministrators.builder()
                                        .chatId(chatId)
                                        .build()
                        );
                        chatUsernames = execute(
                                GetChat.builder()
                                        .chatId(chatId)
                                        .build()
                        ).getActiveUsernames();
                    } catch (TelegramApiException e) {
                        log.error("failed to get chat users, update: {}", update);
                        throw new RuntimeException(e);
                    }
                    Set<String> allActiveUsers = new HashSet<>();
                    if (chatAdmins != null && !chatAdmins.isEmpty()) {
                        allActiveUsers.addAll(chatAdmins.stream()
                                .map(chatMember -> chatMember.getUser().getUserName())
                                .toList());
                    }
                    if (chatUsernames != null && !chatUsernames.isEmpty()) {
                        allActiveUsers.addAll(chatUsernames);
                    }
                    allActiveUsers.remove(appProperties.getBot().getUsername());
                    responseMsg = new StringBuilder("pinging everyone: ")
                            .append(allActiveUsers.stream()
                                    .map(s -> "@" + s)
                                    .collect(Collectors.joining(", ")))
                            .toString();
                    }
                }
            case "/dadbot" -> {
                ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                chatConfig.toggleDadBot();
                chatConfigService.save(chatConfig);
                responseMsg = "Dad bot is " + (Boolean.TRUE.equals(chatConfig.getDadBot())? "enabled" : "disabled");
            }
            default -> {
                if (lowerCaseMessage.startsWith("/translate")) {
                    if (lowerCaseMessage.length() == 10) {
                        chatConfigService.setLanguage(message.getChatId(), Language.NONE);
                        responseMsg = "translation disabled";
                    } else {
                        String language = "";
                        try {
                            language = lowerCaseMessage.substring(11);
                            chatConfigService.setLanguage(message.getChatId(), fromCode(language));
                        } catch (IndexOutOfBoundsException e) {
                            log.warn("Weird language input, message text: {}", lowerCaseMessage);
                            responseMsg = "Please specify the target language /translate <language code>\nExample: /translate en";
                        } catch (IllegalArgumentException e) {
                            try {
                                chatConfigService.setLanguage(message.getChatId(), Language.valueOf(language.toUpperCase()));
                            } catch (IllegalArgumentException e1) {
                                log.warn("Unknown language, message text: {}", lowerCaseMessage);
                                responseMsg = "Unknown language! List of available languages: " +
                                        String.join(", ", Arrays.stream(Language.values()).map(Language::getCode).toList());
                            }
                        }
                    }
                }
            }
        }

        if (StringUtils.isNoneBlank(responseMsg)) {
            try {
                executeAsync(
                        SendMessage.builder()
                                .chatId(message.getChatId())
                                .text(responseMsg)
                                .build()
                );
            } catch (TelegramApiException e) {
                log.error("failed to send message, update: {}", update);
                throw new RuntimeException(e);
            }
        }
    }

    private void handleCallbackQuery(Update update) {
        Message message = update.getCallbackQuery().getMessage();

        InlineKeyboardMarkup replyMarkup = null;
        String responseMsg = null;
        CallbackDataEnum callbackDataEnum = null;

        try {
            callbackDataEnum = CallbackDataEnum.valueOf(update.getCallbackQuery().getData());
        } catch (IllegalArgumentException e) {
            Language language = Language.valueOf(update.getCallbackQuery().getData());
            ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
            chatConfig.setTranslationTargetLang(language);
            chatConfigService.save(chatConfig);
            responseMsg = MessagesEnum.START_MESSAGE.getValue();
            replyMarkup = infoKeyboard(chatConfig.getDadBot(), chatConfig.getTranslationTargetLang());
        }

        if (callbackDataEnum != null) {
            switch (callbackDataEnum) {
                case MAIN_MENU -> {
                    ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                    responseMsg = MessagesEnum.START_MESSAGE.getValue();
                    replyMarkup = infoKeyboard(chatConfig.getDadBot(), chatConfig.getTranslationTargetLang());
                }
                case TRANSLATION_SELECTOR -> {
                    ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                    replyMarkup = getKeyboardsMap().get(LANG_SELECT_MSG);
                    responseMsg = String.format(LANG_SELECT_MSG.getValue(), chatConfig.getTranslationTargetLang().getCode());
                }
                case DAD_BOT -> {
                    ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                    chatConfig.toggleDadBot();
                    chatConfigService.save(chatConfig);
                    responseMsg = message.getText();
                    replyMarkup = infoKeyboard(chatConfig.getDadBot(), chatConfig.getTranslationTargetLang());
                }
                case EXIT -> {
                    try {
                        executeAsync(
                                DeleteMessage.builder()
                                        .chatId(message.getChatId())
                                        .messageId(message.getMessageId())
                                        .build()
                        );
                    } catch (TelegramApiException e) {
                        log.error("failed to delete message, update {}", update);
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        if (replyMarkup != null && responseMsg != null)
            try {
                executeAsync(
                        EditMessageText.builder()
                                .chatId(message.getChatId())
                                .messageId(message.getMessageId())
                                .text(responseMsg)
                                .replyMarkup(replyMarkup)
                                .build()
                );
            } catch (TelegramApiException e) {
                log.error("failed to update message, update {}", update);
                throw new RuntimeException(e);
            }
    }

    private void handleTextMessage(String text, Update update) {
        Message message = update.getMessage();
        ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());

        //Dad bot
        if (Boolean.TRUE.equals(chatConfig.getDadBot())) {
            int index = text.indexOf("I'm ");
            if (index != -1) {
                try {
                    executeAsync(
                            SendMessage.builder()
                                    .replyToMessageId(message.getMessageId())
                                    .chatId(message.getChatId())
                                    .text(new StringBuilder("Hi ")
                                            .append(text.substring(index + 4))
                                            .append(", I'm dad")
                                            .toString())
                                    .build()
                    );
                } catch (TelegramApiException e) {
                    log.error("failed to send dad bot message, update {}, chatConfig {}", update, chatConfig);
                    throw new RuntimeException(e);
                }
            }
        }

        //Translation
        if (chatConfig.getTranslationTargetLang() != null){
            String translate = Translator.translate(chatConfig.getTranslationTargetLang(), text);
            if (!translate.equals(text)) {
                try {
                    executeAsync(
                            SendMessage.builder()
                                    .replyToMessageId(message.getMessageId())
                                    .chatId(message.getChatId())
                                    .text(translate)
                                    .build()
                    );
                } catch (TelegramApiException | IllegalArgumentException e) {
                    log.error("failed execute translation, update {}, chatConfig {}", update, chatConfig);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void handlePhoto(Message message) {
        if (message.getCaption().toLowerCase().startsWith("/ocr")) {
            byte[] fileData;
            try {
                fileData = getFileData(message);
            } catch (TelegramApiException | IOException e) {
                log.error("failed to load file, message: {}", message);
                throw new RuntimeException(e);
            }
            String responseMsg = "Failed to process image";
            if (fileData.length > 0) {
                Optional<String> textFromImage = ocrService.getTextFromImage(fileData);
                if (textFromImage.isPresent()) {
                    responseMsg = textFromImage.get();
                }
            }

            try {
                executeAsync(
                        SendMessage.builder()
                                .replyToMessageId(message.getMessageId())
                                .chatId(message.getChatId())
                                .text(responseMsg)
                                .build()
                );
            } catch (TelegramApiException | IllegalArgumentException e) {
                log.error("failed to send response message, message {}", message);
                throw new RuntimeException(e);
            }
        }
    }

    private byte[] getFileData(Message message) throws TelegramApiException, IOException {
        List<PhotoSize> photo = message.getPhoto();
        if (!photo.isEmpty()) {
            String fileId = photo.get(photo.size() - 1).getFileId();
            File execute = execute(
                    GetFile.builder()
                            .fileId(fileId)
                            .build()
            );
            return Files.readAllBytes(downloadFile(execute.getFilePath()).toPath());
        }
        throw new TelegramApiException("No photos found");
    }

    @Override
    public String getBotUsername() {
        return appProperties.getBot().getUsername();
    }
}