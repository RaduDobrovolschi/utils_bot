package com.utilsbot.bots;

import com.utilsbot.config.AppProperties;
import com.utilsbot.domain.ChatConfig;
import com.utilsbot.domain.UserData;
import com.utilsbot.domain.enums.CallbackDataEnum;
import com.utilsbot.domain.enums.MessagesEnum;
import com.utilsbot.domain.enums.OcrLanguages;
import com.utilsbot.domain.enums.TimeUnits;
import com.utilsbot.service.*;
import com.utilsbot.service.dto.ExpectingInputDto;
import com.utilsbot.service.dto.LocationResponseDTO;
import com.utilsbot.service.dto.ResponseMsgDataDTO;
import jakarta.annotation.PostConstruct;
import net.suuft.libretranslate.Language;
import net.suuft.libretranslate.Translator;
import org.apache.commons.lang3.StringUtils;
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
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

import static com.utilsbot.domain.enums.InputType.*;
import static com.utilsbot.domain.enums.MessagesEnum.*;
import static com.utilsbot.keyboard.CustomKeyboards.*;
import static com.utilsbot.keyboard.KeyboardHelper.updateHour;
import static com.utilsbot.keyboard.KeyboardHelper.updateMinute;
import static com.utilsbot.utils.AppUtils.*;
import static com.utilsbot.utils.TimeUtils.*;

/*
 *
 * todo list of shit that can be added
 *   notifications -> this one is gonna be a pain
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
    private final NotificationService notificationService;
    private final ExpectingInputService expectingInputService;
    private final LocationService locationService;
    private final OcrService ocrService;
    private final TranscriptionService transcriptionService;
    private final Executor asyncExecutor;

    public UtilsBot(AppProperties appProperties,
                    ChatConfigService chatConfigService,
                    UserDataService userDataService,
                    NotificationService notificationService,
                    ExpectingInputService expectingInputService,
                    LocationService locationService,
                    OcrService ocrService,
                    TranscriptionService transcriptionService,
                    @Qualifier("taskExecutor") Executor asyncExecutor) {
        super(appProperties.getBot().getToken());
        this.appProperties = appProperties;
        this.chatConfigService = chatConfigService;
        this.userDataService = userDataService;
        this.notificationService = notificationService;
        this.expectingInputService = expectingInputService;
        this.locationService = locationService;
        this.ocrService = ocrService;
        this.transcriptionService = transcriptionService;
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
            e.printStackTrace();
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
                if (message.hasVoice()) {
                    handleVoice(message);
                }
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        });
    }

    private void handleCommand(String lowerCaseMessage, Update update) {
        Message message = update.getMessage();
        if (!message.getChat().getType().equals(PRIVATE)) {
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
                if (message.getChat().getType().equals(PRIVATE))
                    responseMsg = "this command is only available in group chats";
                else {
                    Optional<UserData> userData = userDataService.handleCommand(message.getChatId(), message.getFrom().getId());
                    try {
                        executeAsync(
                            SendMessage.builder()
                                    .chatId(message.getChatId())
                                    .replyToMessageId(message.getMessageId())
                                    .text("notifications " + (userData.isPresent()? ENABLED : DISABLED))
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
                responseMsg = "Dad bot is " + (Boolean.TRUE.equals(chatConfig.getDadBot())? ENABLED : DISABLED);
            }
            default -> {
                if (lowerCaseMessage.startsWith("/translate")) {
                    if (lowerCaseMessage.length() == 10) {
                        if (chatConfigService.setLanguage(message.getChatId(), Language.NONE)) {
                            responseMsg = "translation disabled";
                        } else {
                            responseMsg = "Please specify the target language /translate <language code>\nExample: /translate en";
                        }
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

        if (callbackData.startsWith("NF_")) {
            handleCalendarCallback(callbackData.substring(3), update);
            return;
        }

        CallbackDataEnum callbackDataEnum = null;
        ResponseMsgDataDTO responseMsg = null;

        try {
            callbackDataEnum = CallbackDataEnum.valueOf(callbackData);
        } catch (IllegalArgumentException e) {
            Language language = Language.valueOf(callbackData);
            ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
            chatConfig.setTranslationTargetLang(language);
            chatConfigService.save(chatConfig);
            responseMsg = new ResponseMsgDataDTO(
                    MessagesEnum.START_MESSAGE.getValue(),
                    infoKeyboard(chatConfig.getDadBot(), chatConfig.getTranslationTargetLang())
            );
            answerCallbackQuery("Translation language set to " + language, update.getCallbackQuery());
        }

        if (callbackDataEnum != null) {
            switch (callbackDataEnum) {
                case MAIN_MENU -> {
                    ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                    responseMsg = new ResponseMsgDataDTO(
                            MessagesEnum.START_MESSAGE.getValue(),
                            infoKeyboard(chatConfig.getDadBot(), chatConfig.getTranslationTargetLang())
                    );
                }
                case TRANSLATION_SELECTOR -> {
                    ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                    responseMsg = new ResponseMsgDataDTO(
                            String.format(LANG_SELECT_MSG.getValue(),
                            chatConfig.getTranslationTargetLang().getCode()), getKeyboard(LANG_SELECT_MSG)
                    );
                }
                case DAD_BOT -> {
                    ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                    chatConfig.toggleDadBot();
                    chatConfigService.save(chatConfig);
                    boolean dadBot = chatConfig.getDadBot();
                    responseMsg = new ResponseMsgDataDTO(
                            message.getText(),
                            infoKeyboard(dadBot, chatConfig.getTranslationTargetLang())
                    );
                    answerCallbackQuery("Dad bot " + (dadBot? ENABLED : DISABLED), update.getCallbackQuery());
                }
                case TIME_REGION_UPDATE_NAME -> {
                    Message msg = sendTextRequestMsg(update, String.format(REQUEST_MSG_UPDATE.getValue(), "ur location/city name"));
                    expectingInputService.addExpectingInput(
                            ExpectingInputDto.builder()
                                    .userIdAndChatId(update.getCallbackQuery())
                                    .inputType(LOCATION_UPDATE)
                                    .previousMsg(REQUEST_MSG_UPDATE, msg)
                                    .build()
                    );
                }
                case NOTIFICATIONS_CONFIG -> {
                    if (message.getChat().getType().equals(PRIVATE)) {
                        userDataService.addPrivateChatUser(message.getChatId(), update.getCallbackQuery().getFrom().getId());
                    }
                    responseMsg = chatConfigService.createNotificationsMsg(message.getChatId(), this);
                }
                case PING_EVERYONE -> responseMsg = new ResponseMsgDataDTO(handleEveryone(message));
                case UPDATE_USER_GROUP -> {
                    if(message.getChat().getType().equals(PRIVATE)) {
                        answerCallbackQuery("only for group chats", update.getCallbackQuery());
                        return;
                    }

                    Optional<UserData> userData = userDataService.handleCommand(message.getChatId(), update.getCallbackQuery().getFrom().getId());
                    String updatedMsgText;
                    String username = getUsername(message.getChatId(), update.getCallbackQuery().getFrom().getId());
                    String text = message.getText();
                    String userNames = text.substring(text.indexOf("notifications:") + 14);
                    if (userData.isPresent()) {
                        updatedMsgText = new StringBuilder(message.getText())
                                .append(userNames.length() == 0? "\n" : ", ")
                                .append(username)
                                .toString();
                    } else {
                        updatedMsgText = String.format(EVERYONE_CONFIG.getValue(), 
                                userNames.replaceFirst(username, "")
                                        .replaceFirst(",,", "")
                        );
                    }
                    responseMsg = new ResponseMsgDataDTO(
                        updatedMsgText, message.getReplyMarkup()
                    );
                    answerCallbackQuery("notifications " + (userData.isPresent()? ENABLED : DISABLED), update.getCallbackQuery());
                }
                case CANCEL -> {
                    expectingInputService.removeExpectingInput(message.getChatId());
                    deleteMsg(message);
                }
                case ADD_NOTIFICATION -> {
                    ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                    if (!chatConfig.hasGtmOffset()) {
                        answerCallbackQuery("please set ur time region first", update.getCallbackQuery());
                        return;
                    }

                    if (message.getText().equals(SEL_NOTIFY_MIN.getValue())) {
                        responseMsg = new ResponseMsgDataDTO(
                            SEL_NOTIFY_HOUR.getValue(),
                            message.getReplyMarkup()
                        );
                    } else {
                        int month;
                        int year;
                        ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(update.getCallbackQuery().getFrom().getId());
                        if (expectingInput != null && expectingInput.inputType().equals(NF_BUILD) &&
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
                                    ExpectingInputDto.builder()
                                            .userIdAndChatId(update.getCallbackQuery())
                                            .notificationTimeData(TimeUnits.YEAR, year)
                                            .notificationTimeData(TimeUnits.MONTH, month)
                                            .inputType(NF_BUILD)
                                            .build()
                            );
                        }
                        responseMsg = new ResponseMsgDataDTO(
                                SEL_NOTIFY_DAY.getValue(),
                                dayOfMonthKeyboard(year, month)
                        );
                    }
                }
                case SELECT_MONTH -> responseMsg = new ResponseMsgDataDTO(
                        SEL_NOTIFY_MONTH.getValue(),
                        monthsKeyboard()
                );
                case T_DEC -> responseMsg = handleTimeKeyboardUpdate(message, time -> --time);
                case T_INC -> responseMsg = handleTimeKeyboardUpdate(message, time -> ++time);
                case T_ADD_5 -> responseMsg = handleTimeKeyboardUpdate(message, time -> time + 5);
                case T_SUB_5 -> responseMsg = handleTimeKeyboardUpdate(message, time -> time - 5);
                case T_ADD_10 -> responseMsg = handleTimeKeyboardUpdate(message, time -> time + 10);
                case T_SUB_10 -> responseMsg = handleTimeKeyboardUpdate(message, time -> time - 10);
                case T_MANUAL_INPUT -> {
                    ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(message.getChatId());
                    if (expectingInput != null && expectingInput.inputType().equals(NF_BUILD) &&
                        expectingInput.notificationTimeData().isPresent()) {

                        Message msg = sendTextRequestMsg(update, String.format(REQUEST_MSG_UPDATE.getValue(), "notification time like hh:mm"));
                        expectingInputService.addExpectingInput(
                                ExpectingInputDto.builder()
                                        .userIdAndChatId(update.getCallbackQuery())
                                        .inputType(NF_SET_MANUAL_TIME)
                                        .previousMsg(REQUEST_MSG_UPDATE, msg)
                                        .previousMsg(EVERYONE_CONFIG, message)
                                        .notificationTimeData(expectingInput.notificationTimeData().get())
                                        .build()
                        );
                    }
                }
                case NEXT_UPDATE_TIME -> {
                    if (message.getText().equals(SEL_NOTIFY_HOUR.getValue())) {
                        responseMsg = new ResponseMsgDataDTO(
                                SEL_NOTIFY_MIN.getValue(),
                                message.getReplyMarkup()
                        );
                    } else {
                        ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(message.getChatId());
                        if (expectingInput != null && expectingInput.inputType().equals(NF_BUILD) &&
                            expectingInput.notificationTimeData().isPresent()) {

                            ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                            String[] split = message.getReplyMarkup().getKeyboard().get(0).get(1).getText().split(":");
                            LocalDateTime inputTime = expectingInput.createDateTime(
                                    Integer.parseInt(split[0]),
                                    Integer.parseInt(split[1])
                            );
                            LocalDateTime userTime = chatConfig.getUserTime();

                            if (inputTime != null && userTime.isAfter(inputTime)) {
                                answerCallbackQuery("notification time can't be in the past", update.getCallbackQuery());
                                return;
                            } else {
                                notificationService.addNotification(removeOffset(chatConfig.getGmtOffset(), inputTime), chatConfig.getId());
                                answerCallbackQuery("notification added successfully", update.getCallbackQuery());
                            }
                        }
                        responseMsg = chatConfigService.createNotificationsMsg(message.getChatId(), this);
                    }
                }
                case DELETE_NF -> {
                    ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(message.getChatId());
                    if (expectingInput != null && expectingInput.inputType().equals(NF_UPDATE) &&
                        expectingInput.notificationId().isPresent()) {
                        Long nId = expectingInput.notificationId().get();
                        notificationService.delete(nId);
                        answerCallbackQuery("notification deleted successfully", update.getCallbackQuery());
                    }
                    responseMsg = chatConfigService.createNotificationsMsg(message.getChatId(), this);
                }
                case ADD_NF_CUSTOM_MSG -> {
                    Message msg = sendTextRequestMsg(update, String.format(REQUEST_MSG_UPDATE.getValue(), "ur custom msg"));
                    ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(message.getChatId());
                    if (expectingInput != null && expectingInput.inputType().equals(NF_UPDATE) &&
                        expectingInput.notificationId().isPresent()) {
                        expectingInputService.addExpectingInput(
                                ExpectingInputDto.builder()
                                        .userIdAndChatId(update.getCallbackQuery())
                                        .inputType(NF_SET_CUSTOM_MSG)
                                        .previousMsg(REQUEST_MSG_UPDATE, msg)
                                        .notificationId(expectingInput.notificationId().get())
                                        .build()
                        );
                    }
                }
                case EXIT -> deleteMsg(message);
                case IGNORE -> { return; }
            }
        }

        genericUpdateMsg(message, responseMsg);
    }

    private ResponseMsgDataDTO handleTimeKeyboardUpdate(Message message, IntUnaryOperator timeUpdate) {
        ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(message.getChatId());
        if (expectingInput != null &&
           (expectingInput.inputType().equals(NF_BUILD) || expectingInput.inputType().equals(NF_UPDATE)) &&
            expectingInput.notificationTimeData().isPresent()) {

            InlineKeyboardMarkup replyMarkup = message.getReplyMarkup();
            if (message.getText().equals(SEL_NOTIFY_HOUR.getValue())) {
                updateHour(replyMarkup, timeUpdate);
                return new ResponseMsgDataDTO(
                        SEL_NOTIFY_HOUR.getValue(),
                        replyMarkup
                );
            } else {
                updateMinute(replyMarkup, timeUpdate);
                return new ResponseMsgDataDTO(
                        SEL_NOTIFY_MIN.getValue(),
                        replyMarkup
                );
            }
        } else
            return chatConfigService.createNotificationsMsg(message.getChatId(), this);
    }

    private void handleCalendarCallback(String callbackData, Update update) {
        Message message = update.getCallbackQuery().getMessage();

        ResponseMsgDataDTO responseMsg = null;
        boolean errorFlag = false;

        if (callbackData.startsWith("Y_") && !callbackData.contains("D_")) {
            int year = getDataFromCallback("Y_", callbackData);
            int month = getDataFromCallback("M_", callbackData);

            responseMsg = new ResponseMsgDataDTO(
                SEL_NOTIFY_DAY.getValue(),
                dayOfMonthKeyboard(year, month)
            );
        } else if (callbackData.startsWith("Y_")) {
            int year = getDataFromCallback("Y_", callbackData);
            int month = getDataFromCallback("M_", callbackData);
            int day = getDataFromCallback("D_", callbackData);

            LocalDate inputDate = LocalDate.of(year, month, day);
            ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
            LocalDate userDate = chatConfig.getUserDate();

            if (userDate.isAfter(inputDate)) {
                answerCallbackQuery("can't chose past date", update.getCallbackQuery());
                return;
            }

            ExpectingInputDto expectingInput = expectingInputService.getExpectingInput(message.getChatId());
            if (expectingInput != null && expectingInput.inputType().equals(NF_BUILD)) {
                errorFlag = !expectingInput.updateNotificationTimeData(TimeUnits.YEAR, year) ||
                            !expectingInput.updateNotificationTimeData(TimeUnits.MONTH, month) ||
                            !expectingInput.updateNotificationTimeData(TimeUnits.DAY, day);
            } else errorFlag = true;

            LocalDateTime userTime = chatConfig.getUserTime();
            responseMsg = new ResponseMsgDataDTO(
                    SEL_NOTIFY_HOUR.getValue(),
                    timeKeyboard(userTime.getHour(), userTime.getMinute())
            );
        } else if (callbackData.startsWith("ID_")) {
            long id = getIdFromCallback("ID_", callbackData);
            responseMsg = notificationService.createEditNotificationMsg(id);
            if (responseMsg != null) {
                expectingInputService.addExpectingInput(
                        ExpectingInputDto.builder()
                                .userIdAndChatId(update.getCallbackQuery())
                                .inputType(NF_UPDATE)
                                .notificationId(id)
                                .build()
                );
            }
        }

        if (errorFlag) {
            answerCallbackQuery("failed to build notification", update.getCallbackQuery());
            responseMsg = chatConfigService.createNotificationsMsg(message.getChatId(), this);
        }
        genericUpdateMsg(message, responseMsg);
    }

    private void handleTextMessage(String text, Update update) {
        Message message = update.getMessage();
        ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());

        //Dad bot
        if (Boolean.TRUE.equals(chatConfig.getDadBot())) {
            int index = indexOfByRegex("[Ii]['’]m ", text);
            int length = 4;
            if (index == -1) {
                index = indexOfByRegex("[Ii]m ", text);
                length = 3;
            }
            if (index != -1) {
                try {
                    executeAsync(
                            SendMessage.builder()
                                    .replyToMessageId(message.getMessageId())
                                    .chatId(message.getChatId())
                                    .text(new StringBuilder("Hi ")
                                            .append(text.substring(index + length))
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
        String responseMsg = null;
        switch (expectingInput.inputType()) {
            case LOCATION_UPDATE -> {
                //todo add option to process timezone_abbreviation and gmt_offset as user input
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
            }
            case NF_SET_CUSTOM_MSG -> {
                expectingInput.notificationId().ifPresent(nfId ->
                        notificationService.setCustomMsg(nfId, message.getMessageId())
                );
                responseMsg = "notification msg saved";
            }
            case NF_SET_MANUAL_TIME -> {
                Calendar calendar = Calendar.getInstance();
                try {
                    Date date = timeInputFormat.parse(message.getText());
                    calendar.setTime(date);
                } catch (ParseException e) {
                    log.warn("failed to process time {}, e: {}", message.getText(), e.getMessage());
                    responseMsg = "invalid time format";
                }

                if (StringUtils.isBlank(responseMsg)) {
                    LocalDateTime inputTime = expectingInput.createDateTime(
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE)
                    );
                    ChatConfig chatConfig = chatConfigService.getChatConfig(message.getChatId());
                    LocalDateTime userTime = chatConfig.getUserTime();

                    if (inputTime != null && userTime.isAfter(inputTime)) {
                        responseMsg = "failed to update time";
                    } else {
                        notificationService.addNotification(removeOffset(chatConfig.getGmtOffset(), inputTime), chatConfig.getId());
                        responseMsg = "notification set for " + inputTime.format(defaultDTFormatter);
                    }
                    expectingInput.previousMsg().ifPresent(prevMsgMap -> {
                        Message menuMsg = prevMsgMap.get(EVERYONE_CONFIG);
                        if (menuMsg != null) {
                            genericUpdateMsg(menuMsg, chatConfigService.createNotificationsMsg(message.getChatId(), this));
                        }
                    });
                }
            }
        }

        if (StringUtils.isNotBlank(responseMsg)) {
            try {
                executeAsync(
                        SendMessage.builder()
                                .replyToMessageId(message.getMessageId())
                                .chatId(message.getChatId())
                                .text(responseMsg)
                                .build()
                );
            } catch (TelegramApiException | IllegalArgumentException e) {
                log.error("failed update location, message {}, ExpectingInputDto {}", message, expectingInput);
                e.printStackTrace();
            }
        }
        if (expectingInput.previousMsg().isPresent() && expectingInput.previousMsg().get().get(REQUEST_MSG_UPDATE) != null) {
            Message prevMsg = expectingInput.previousMsg().get().get(REQUEST_MSG_UPDATE);
            try {
                executeAsync(
                        EditMessageText.builder()
                                .chatId(prevMsg.getChatId())
                                .messageId(prevMsg.getMessageId())
                                .entities(prevMsg.getEntities())
                                .text(prevMsg.getText())
                                .replyMarkup(null)
                                .build()
                );
            } catch (TelegramApiException e) {
                log.error("failed to update previous msg {}", prevMsg);
                e.printStackTrace();
            }
        }
        expectingInputService.removeExpectingInput(message.getChatId());
    }

    private String handleEveryone(Message message) {
        String errorMsg = null;
        if (message.getChat().getType().equals(PRIVATE))
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

    public Message sendTextRequestMsg(Update update, String text) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .replyMarkup(getKeyboard(REQUEST_MSG_UPDATE))
                .chatId(callbackQuery.getMessage().getChatId());
        addUserMentions(sendMessageBuilder, names -> names.append(text).toString(), callbackQuery.getFrom());
        try {
            return execute(sendMessageBuilder.build());
        } catch (TelegramApiException e) {
            log.error("failed to send message, update {}", update);
            e.printStackTrace();
        }
        return null;
    }

    private void handlePhoto(Message message) {
        String command = message.getCaption().toLowerCase();
        if (command.startsWith("/ocr")) {
            String[] spit = command.split(" ");
            String langStr = spit.length > 1? spit[1] : "";
            OcrLanguages lang = OcrLanguages.of(langStr);
            byte[] fileData;
            try {
                fileData = getPhotoData(message);
            } catch (TelegramApiException | IOException e) {
                log.error("failed to load file, message: {}", message);
                e.printStackTrace();
                return;
            }
            String responseText = "Failed to process image";
            if (fileData.length > 0) {
                Optional<String> textFromImage = ocrService.getTextFromImage(fileData, lang);
                if (textFromImage.isPresent()) {
                    responseText = textFromImage.get();
                }
            }

            respondToMsg(message, responseText);
        }
    }

    private void handleVoice(Message message) {
        String voice = "failed to extract voice";
        try {
            voice = transcriptionService.getTranscription(getVoiceData(message));
        } catch (TelegramApiException e) {
            log.error("failed to extract voice {}", message);
            e.printStackTrace();
        }
        respondToMsg(message, voice);
    }

    private java.io.File getVoiceData(Message message) throws TelegramApiException {
        File execute = execute(
                GetFile.builder()
                        .fileId(message.getVoice().getFileId())
                        .build()
        );
        return downloadFile(execute.getFilePath());
    }

    public void respondToMsg(Message message, String text) {
        try {
            executeAsync(
                    SendMessage.builder()
                            .replyToMessageId(message.getMessageId())
                            .chatId(message.getChatId())
                            .text(text)
                            .build()
            );
        } catch (TelegramApiException | IllegalArgumentException e) {
            log.error("failed to send response message {}", message);
            e.printStackTrace();
        }
    }

    private byte[] getPhotoData(Message message) throws TelegramApiException, IOException {
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

    private void genericUpdateMsg(Message message, ResponseMsgDataDTO responseMsgDataDTO) {
        if (responseMsgDataDTO != null && message != null)
            genericUpdateMsg(message, responseMsgDataDTO.responseMsg(), responseMsgDataDTO.replyMarkup());
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

    public String getUsername(UserData user) {
        return getUsername(user.getChatConfig().getId(), user.getUserId());
    }

    public String getUsername(Long chatId, Long userId) {
        try {
            User result = execute(
                    GetChatMember.builder()
                            .chatId(chatId)
                            .userId(userId)
                            .build()
            ).getUser();
            return result.getUserName() == null ? result.getFirstName() : result.getUserName();
        } catch (TelegramApiException e) {
            log.error("failed to get user: chatId {}, userId {}", chatId, userId);
            e.printStackTrace();
        }
        return "";
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