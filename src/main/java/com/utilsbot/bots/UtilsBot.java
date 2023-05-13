package com.utilsbot.bots;

import com.utilsbot.config.AppProperties;
import com.utilsbot.domain.ChatConfig;
import com.utilsbot.domain.UserData;
import com.utilsbot.domain.enums.CallbackDataEnum;
import com.utilsbot.domain.enums.InputType;
import com.utilsbot.domain.enums.MessagesEnum;
import com.utilsbot.domain.enums.TimeUnits;
import com.utilsbot.service.*;
import com.utilsbot.service.dto.ExpectingInputDto;
import com.utilsbot.service.dto.LocationResponseDTO;
import jakarta.annotation.PostConstruct;
import net.suuft.libretranslate.Language;
import net.suuft.libretranslate.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static com.utilsbot.domain.enums.MessagesEnum.*;
import static com.utilsbot.keyboard.CustomKeyboards.*;
import static com.utilsbot.keyboard.KeyboardHelper.*;
import static com.utilsbot.utils.AppUtils.fromCode;
import static com.utilsbot.utils.AppUtils.getDataFromCallback;

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
    private final UserDataService userDataService;
    private final ExpectingInputService expectingInputService;
    private final LocationService locationService;
    private final OcrService ocrService;
    private final Executor asyncExecutor;

    public UtilsBot(AppProperties appProperties,
                    ChatConfigService chatConfigService,
                    UserDataService userDataService,
                    ExpectingInputService expectingInputService,
                    LocationService locationService,
                    OcrService ocrService,
                    @Qualifier("taskExecutor") Executor asyncExecutor) {
        super(appProperties.getBot().getToken());
        this.appProperties = appProperties;
        this.chatConfigService = chatConfigService;
        this.userDataService = userDataService;
        this.expectingInputService = expectingInputService;
        this.locationService = locationService;
        this.ocrService = ocrService;
        this.asyncExecutor = asyncExecutor;
    }

    @PostConstruct
    private void postConstruct() {
        log.debug("Setting bot commands");
        try {
//            executeAsync(
//                    DeleteMyCommands.builder().build()
//            );
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

        //todo add handling for users leaving chat or bot getting kicked
        asyncExecutor.execute(() -> { //should consider to async only handlePhoto()
            if (update.hasMessage()) {
                Message message = update.getMessage();

                //ignore messages older than 1 hour
                if ((int)(new Date().getTime()/1000) - message.getDate() > 3600)
                    return;

                if (message.hasText()) {
                    String text = message.getText();
                    if (expectingInputService.hasExpectingInput(message.getChatId())) {
                        handleMsgResponse(message);
                    } else {
                        if (text.startsWith("/")) {
                            handleCommand(text.toLowerCase(), update);
                        } else {
                            handleTextMessage(text, update);
                        }
                    }
                }
                if (message.hasPhoto()) {
                    handlePhoto(message);
                }
                //todo mb implement later
//                if (message.getChat().getType().equals("private") && message.hasLocation()) {
//                    handleLocation(message);
//                }
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        });
    }

    private void handleCommand(String lowerCaseMessage, Update update) {
        Message message = update.getMessage();
        if (!message.getChat().getType().equals("private")) {
            lowerCaseMessage = lowerCaseMessage.replace(("@" + appProperties.getBot().getUsername().toLowerCase()), "");
        }
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
                    e.printStackTrace();
                }
            }
            case "/notifications" -> {
                if (message.getChat().getType().equals("private"))
                    responseMsg = "this command is only available in group chats";
                else {
                    Optional<UserData> userData = userDataService.handleCommand(message.getChatId(), message.getFrom().getId());
                    try {
                        executeAsync(
                            SendMessage.builder()
                                    .chatId(message.getChatId())
                                    .replyToMessageId(message.getMessageId())
                                    .text("notifications " + (userData.isPresent()? "enabled" : "disabled"))
                                    .build()
                        );
                    } catch (TelegramApiException e) {
                        log.error("failed to respond to message. update: {}", update);
                        e.printStackTrace();
                    }
                }
            }
            case "/everyone" -> responseMsg = handleEveryone(message);
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

        genericUpdateMsg(message, responseMsg, null);
    }

    private void handleCallbackQuery(Update update) {
        Message message = update.getCallbackQuery().getMessage();
        String callbackData = update.getCallbackQuery().getData();

        InlineKeyboardMarkup replyMarkup = null;
        String responseMsg = null;
        CallbackDataEnum callbackDataEnum = null;

        if (callbackData.startsWith("NF_")) {
            handleCalendarCallback(callbackData.substring(3), update);
            return;
        }

        try {
            callbackDataEnum = CallbackDataEnum.valueOf(callbackData);
        } catch (IllegalArgumentException e) {
            Language language = Language.valueOf(callbackData);
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
                    replyMarkup = getKeyboard(LANG_SELECT_MSG);
                    responseMsg = String.format(LANG_SELECT_MSG.getValue(), chatConfig.getTranslationTargetLang().getCode());
                }
                case DAD_BOT -> {
                    ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                    chatConfig.toggleDadBot();
                    chatConfigService.save(chatConfig);
                    responseMsg = message.getText();
                    replyMarkup = infoKeyboard(chatConfig.getDadBot(), chatConfig.getTranslationTargetLang());
                    //todo add AnswerCallbackQuery
                }
//                case TIME_REGION_UPDATE_CORDS -> {
//                    User from = update.getCallbackQuery().getFrom();
//                    try {
//                        executeAsync(
//                                SendMessage.builder()
//                                        .text(SHARE_LOCATION.getValue())
//                                        .chatId(from.getId())
//                                        .replyMarkup(requestLocationKeyboard)
//                                        .build()
//                        );
//                        chatConfigService.addExpectingInput(message.getChatId(), from.getUserName());
//                    } catch (TelegramApiException e) {
//                        log.error("failed to send ReplyKeyboardMarkup {}", update);
//                        throw new RuntimeException(e);
//                    }
//                }
                case TIME_REGION_UPDATE_NAME -> {
                    User from = update.getCallbackQuery().getFrom();
                    SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                            .replyMarkup(getKeyboard(REQUEST_LOCATION))
                            .chatId(message.getChatId());
                    addUserMentions(sendMessageBuilder, names -> names.append(REQUEST_LOCATION.getValue()).toString(), from);
                    Message sentMsg = null;
                    try {
                        sentMsg = execute(sendMessageBuilder.build());
                    } catch (TelegramApiException e) {
                        log.error("failed to send message, update {}", update);
                        e.printStackTrace();
                    }
                    expectingInputService.addExpectingInput(
                            new ExpectingInputDto(from.getId(), message.getChatId(), InputType.LOCATION_UPDATE, sentMsg)
                    );
                }
                case NOTIFICATIONS_CONFIG -> {
                    responseMsg = EVERYONE_CONFIG.getValue();
                    replyMarkup = notificationConfig();
                }
                case PING_EVERYONE -> responseMsg = handleEveryone(message);
                case  UPDATE_USER_GROUP -> {
                    Optional<UserData> userData = userDataService.handleCommand(message.getChatId(), update.getCallbackQuery().getFrom().getId());
                    answerCallbackQuery("notifications " + (userData.isPresent()? "enabled" : "disabled"), update.getCallbackQuery());
                }
                case  CANCEL_REGION_UPDATE -> {
                    expectingInputService.removeExpectingInput(message.getChatId());
                    deleteMsg(message);
                }
                case ADD_NOTIFICATION -> {
                    User from = update.getCallbackQuery().getFrom();
                    responseMsg = SEL_NOTIFY_DAY.getValue();
                    int month;
                    int year;
                    ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(from.getId());
                    if (expectingInput != null && expectingInput.inputType().equals(InputType.NOTIFICATION_BUILD) &&
                        expectingInput.notificationTimeData().isPresent()) {

                        EnumMap<TimeUnits, Integer> timeUnitsIntegerEnumMap = expectingInput.notificationTimeData().get();
                        month = timeUnitsIntegerEnumMap.get(TimeUnits.MONTH);
                        year = timeUnitsIntegerEnumMap.get(TimeUnits.YEAR);
                    } else {
                        Calendar calendar = Calendar.getInstance();
                        month = calendar.get(Calendar.MONTH);
                        year = calendar.get(Calendar.YEAR);
                        ++month;
                        expectingInputService.addExpectingInput(
                                new ExpectingInputDto(from.getId(), message.getChatId(), InputType.NOTIFICATION_BUILD, year, month)
                        );
                    }
                    replyMarkup = dayOfMonthKeyboard(year, month);
                }
                case SELECT_MONTH -> {
                    responseMsg = SEL_NOTIFY_MONTH.getValue();
                    replyMarkup = monthsKeyboard();
                }
                case UPDATE_HOUR -> {
                    ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(update.getCallbackQuery().getFrom().getId());
                    if (expectingInput != null && expectingInput.inputType().equals(InputType.NOTIFICATION_BUILD) &&
                        expectingInput.notificationTimeData().isPresent() && expectingInput.notificationTimeData().get().get(TimeUnits.HOUR) != null) {
                        Integer hour = expectingInput.notificationTimeData().get().get(TimeUnits.HOUR);
                        replyMarkup = new InlineKeyboardMarkup(getKeyboard(SEL_NOTIFY_HOUR).getKeyboard());
                        restoreHourSelection(replyMarkup, hour);
                        responseMsg = SEL_NOTIFY_HOUR.getValue();

                    } else {
                        answerCallbackQuery("failed to build notification", update.getCallbackQuery());
                        responseMsg = EVERYONE_CONFIG.getValue();
                        replyMarkup = notificationConfig();
                    }
                }
                case SELECT_MIN -> {
                    Optional<Integer> optHour = getHour(message.getReplyMarkup());
                    if (optHour.isEmpty()) {
                        answerCallbackQuery("please select the time", update.getCallbackQuery());
                        return;
                    }
                    ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(update.getCallbackQuery().getFrom().getId());

                    if (expectingInput != null && expectingInput.inputType().equals(InputType.NOTIFICATION_BUILD) &&
                        expectingInput.notificationTimeData().isPresent()) {

                        EnumMap<TimeUnits, Integer> timeUnitsIntegerEnumMap = expectingInput.notificationTimeData().get();
                        ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());

                        Integer hour = optHour.get();
                        LocalDateTime inputTime = LocalDateTime.of(
                                timeUnitsIntegerEnumMap.get(TimeUnits.YEAR),
                                timeUnitsIntegerEnumMap.get(TimeUnits.MONTH),
                                timeUnitsIntegerEnumMap.get(TimeUnits.DAY),
                                hour,
                                0
                        );
                        LocalDateTime userTime = chatConfig.getUserTime();
                        if (inputTime.isBefore(userTime)) {
                            answerCallbackQuery("please select future time", update.getCallbackQuery());
                            return;
                        }

                        timeUnitsIntegerEnumMap.put(TimeUnits.HOUR, hour);

                        responseMsg = SEL_NOTIFY_MIN.getValue();
                        replyMarkup = minuteKeyboard(hour, 0);
                    } else {
                        answerCallbackQuery("failed to build notification", update.getCallbackQuery());
                        responseMsg = EVERYONE_CONFIG.getValue();
                        replyMarkup = notificationConfig();
                    }
                }
                case EXIT -> deleteMsg(message);
                case IGNORE -> { return; }
            }
        }

        genericUpdateMsg(message, responseMsg, replyMarkup);
    }

    //unmaintainable garbage
    private void handleCalendarCallback(String callbackData, Update update) {
        Message message = update.getCallbackQuery().getMessage();

        String responseMsg = null;
        InlineKeyboardMarkup replyMarkup = null;

        boolean errorFlag = false;
        String warnMsg = null;

        if (callbackData.startsWith("Y_") && !callbackData.contains("D_")) {
            int year = getDataFromCallback("Y_", callbackData);
            int month = getDataFromCallback("M_", callbackData);

            responseMsg = SEL_NOTIFY_DAY.getValue();
            replyMarkup = dayOfMonthKeyboard(year, month);

        } else if (callbackData.startsWith("Y_")) {
            int year = getDataFromCallback("Y_", callbackData);
            int month = getDataFromCallback("M_", callbackData);
            int day = getDataFromCallback("D_", callbackData);

            ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(message.getChatId());
            if (expectingInput != null && expectingInput.inputType().equals(InputType.NOTIFICATION_BUILD)) {
                errorFlag = !expectingInput.updateNotificationTimeData(TimeUnits.YEAR, year) ||
                            !expectingInput.updateNotificationTimeData(TimeUnits.MONTH, month) ||
                            !expectingInput.updateNotificationTimeData(TimeUnits.DAY, day);
            } else errorFlag = true;

            responseMsg = SEL_NOTIFY_HOUR.getValue();
            replyMarkup = getKeyboard(SEL_NOTIFY_HOUR);
        } else if (callbackData.startsWith("P_")) {
            responseMsg = SEL_NOTIFY_HOUR.getValue();
            replyMarkup = message.getReplyMarkup();
            updateAmPm(replyMarkup, callbackData);
        } else if (callbackData.startsWith("H_")) {
            responseMsg = SEL_NOTIFY_HOUR.getValue();
            replyMarkup = message.getReplyMarkup();
            updateHour(replyMarkup, callbackData);
        } else if (callbackData.startsWith("MI_")) {

        }


        if (warnMsg != null) {
            answerCallbackQuery(warnMsg, update.getCallbackQuery());
            return;
        }

        if (errorFlag) {
            answerCallbackQuery("failed to build notification", update.getCallbackQuery());
            responseMsg = EVERYONE_CONFIG.getValue();
            replyMarkup = notificationConfig();
        }
        genericUpdateMsg(message, responseMsg, replyMarkup);
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
                    e.printStackTrace();
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
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleMsgResponse(Message message) {
        ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(message.getChatId());
        switch (expectingInput.inputType()) {
            case LOCATION_UPDATE -> {
                //todo add option to process timezone_abbreviation and gmt_offset as user input
                String responseMsg;
                Optional<LocationResponseDTO> locationData = locationService.getLocationData(message.getText());
                if (locationData.isPresent()) {
                    LocationResponseDTO locationResult = locationData.get();
                    chatConfigService.updateGmtOffset(message.getChatId(), locationResult.gmt_offset());
                    responseMsg = String.format(LOCATION_UPDATED.getValue(),
                            locationResult.timezone_location(),
                            locationResult.timezone_abbreviation(),
                            locationResult.gmt_offset() > 0 ? "+" + locationResult.gmt_offset() : locationResult.gmt_offset());
                } else {
                    responseMsg = LOCATION_FAIL.getValue();
                }
                try {
                    executeAsync(
                            SendMessage.builder()
                                    .replyToMessageId(message.getMessageId())
                                    .chatId(message.getChatId())
                                    .text(responseMsg)
                                    .build()
                    );
                    if (expectingInput.previousMsg().isPresent()) {
                        Message prevMsg = expectingInput.previousMsg().get();
                        executeAsync(
                                EditMessageText.builder()
                                        .chatId(prevMsg.getChatId())
                                        .messageId(prevMsg.getMessageId())
                                        .entities(prevMsg.getEntities())
                                        .text(prevMsg.getText())
                                        .replyMarkup(null)
                                        .build()
                        );
                    }
                } catch (TelegramApiException | IllegalArgumentException e) {
                    log.error("failed update location, message {}, ExpectingInputDto {}", message, expectingInput);
                    e.printStackTrace();
                }
            }
        }
        expectingInputService.removeExpectingInput(message.getChatId());
    }


//    private void handleLocation(Message message) {
//        if (message.getReplyToMessage() != null &&
//            message.getReplyToMessage().getFrom().getUserName().equals(appProperties.getBot().getUsername()) &&
//            message.getReplyToMessage().getText().equals(SHARE_LOCATION.getValue())) {
//
//            Location location = message.getLocation();
//
//
//            try {
//                executeAsync(
//                        SendMessage.builder()
//                                .text("")
//                                .chatId(message.getChatId())
//                                .replyMarkup(replyKeyboardRemove)
//                                .build()
//                );
//            } catch (TelegramApiException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

    private String handleEveryone(Message message) {
        String errorMsg = null;
        if (message.getChat().getType().equals("private"))
            errorMsg = "can't use /everyone in private chat";
        else {
            Set<UserData> allUserData = userDataService.getAllUserData(message.getChatId());
            if (allUserData.isEmpty()) {
                errorMsg = "no users to ping, use /notifications to join";
            } else {
                SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                        .chatId(message.getChatId());
                String targetMsg = "pinging everyone: ";
                addUserMentions(sendMessageBuilder,
                        targetMsg.length(),
                        names -> names.insert(0, targetMsg).toString(),
                        allUserData
                );
                try {
                    executeAsync(sendMessageBuilder.build());
                } catch (TelegramApiException e) {
                    log.error("failed to send message {}", message);
                    e.printStackTrace();
                }
            }
        }
        return errorMsg;
    }

    private void handlePhoto(Message message) {
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

    private void genericUpdateMsg(Message msg, String text, InlineKeyboardMarkup replyMarkup) {
        if (text != null && replyMarkup != null) {
            try {
                executeAsync(
                        EditMessageText.builder()
                                .chatId(msg.getChatId())
                                .messageId(msg.getMessageId())
                                .text(text)
                                .replyMarkup(replyMarkup)
                                .build()
                );
            } catch (TelegramApiException e) {
                log.error("failed to update message, msg {}", msg);
                e.printStackTrace();
            }
        } else if (text != null) {
            try {
                executeAsync(
                        SendMessage.builder()
                                .chatId(msg.getChatId())
                                .text(text)
                                .build()
                );
            } catch (TelegramApiException e) {
                log.info("failed to send message msg: {}", msg);
                e.printStackTrace();
            }
        }
    }

    public void deleteMsg(Message msg) {
        try {
            executeAsync(
                    DeleteMessage.builder()
                            .chatId(msg.getChatId())
                            .messageId(msg.getMessageId())
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error("failed to delete message, message {}", msg);
            e.printStackTrace();
        }
    }

    public void answerCallbackQuery(String msg, CallbackQuery prevQuery) {
        try {
            executeAsync(
                    AnswerCallbackQuery.builder()
                            .callbackQueryId(prevQuery.getId())
                            .text(msg)
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error("failed to AnswerCallbackQuery, previous query {}", prevQuery);
        }
    }

    public static void addUserMentions(SendMessage.SendMessageBuilder sendMessageBuilder, Function<StringBuilder, String> stringFormat, User... users) {
        addUserMentions(sendMessageBuilder, 0, stringFormat, users);
    }

    public void addUserMentions(SendMessage.SendMessageBuilder sendMessageBuilder, Function<StringBuilder, String> stringFormat, Set<UserData> userDataList) {
        addUserMentions(sendMessageBuilder, 0, stringFormat, userDataList);
    }
    public void addUserMentions(SendMessage.SendMessageBuilder sendMessageBuilder, Integer offset, Function<StringBuilder, String> stringFormat, Set<UserData> userDataList) {
        List<MessageEntity> entities = new ArrayList<>();
        StringBuilder usersText = new StringBuilder();
        for (UserData userData : userDataList) {
            User user = null;
            try {
                user = execute(
                        GetChatMember.builder()
                                .chatId(userData.getChatConfig().getId())
                                .userId(userData.getUserId())
                                .build()
                ).getUser();
            } catch (TelegramApiException e) {
                log.error("failed to get user: {}", userData);
                e.printStackTrace();
            }
            if (user != null){
                updateEntities(entities, offset, usersText, user);
            }
        }
        usersText.deleteCharAt(usersText.length() - 2);
        sendMessageBuilder.entities(entities)
                .text(stringFormat.apply(usersText));
    }

    public static void addUserMentions(SendMessage.SendMessageBuilder sendMessageBuilder, Integer offset, Function<StringBuilder, String> stringFormat, User... users) {
        List<MessageEntity> entities = new ArrayList<>();
        StringBuilder usersText = new StringBuilder();
        for (User user : users) {
            updateEntities(entities, offset, usersText, user);
        }
        usersText.deleteCharAt(usersText.length() - 2);
        sendMessageBuilder.entities(entities)
                .text(stringFormat.apply(usersText));
    }

    private static void updateEntities(List<MessageEntity> entities, Integer offset, StringBuilder usersText, User user) {
        if (user.getUserName() == null) {
            String firstName = user.getFirstName();
            entities.add(
                    MessageEntity.builder()
                            .type("text_mention")
                            .offset(usersText.length() + offset)
                            .length(firstName.length())
                            .user(user)
                            .build()
            );
            usersText.append(firstName);
        } else {
            usersText.append("@").append(user.getUserName());
        }
        usersText.append(", ");
    }

    @Override
    public String getBotUsername() {
        return appProperties.getBot().getUsername();
    }
}