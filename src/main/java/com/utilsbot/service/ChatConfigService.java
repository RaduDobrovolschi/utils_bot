package com.utilsbot.service;

import com.utilsbot.bots.UtilsBot;
import com.utilsbot.config.AppProperties;
import com.utilsbot.domain.ChatConfig;
import com.utilsbot.domain.UserData;
import com.utilsbot.repository.ChatConfigRepository;
import com.utilsbot.service.dto.ResponseMsgDataDTO;
import jakarta.transaction.Transactional;
import net.suuft.libretranslate.Language;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.utilsbot.config.TimeConfig.defaultDTFormatter;
import static com.utilsbot.domain.enums.CallbackDataEnum.ADD_NOTIFICATION;
import static com.utilsbot.domain.enums.MessagesEnum.EVERYONE_CONFIG;
import static com.utilsbot.keyboard.CustomKeyboards.notificationConfig;
import static com.utilsbot.keyboard.KeyboardHelper.createBtn;

@Service
@Transactional
public class ChatConfigService {

    private final ChatConfigRepository chatConfigRepository;
    private final AppProperties appProperties;

    public ChatConfigService(ChatConfigRepository chatConfigRepository,
                             AppProperties appProperties) {
        this.chatConfigRepository = chatConfigRepository;
        this.appProperties = appProperties;
    }

    public ChatConfig getChatConfig(long chatId){
        return chatConfigRepository.findById(chatId).orElseGet(() ->
                chatConfigRepository.save(new ChatConfig(chatId))
        );
    }

    public void setLanguage(long chatId, Language language) {
        getChatConfig(chatId).setTranslationTargetLang(language);
    }

    public void updateGmtOffset(Long chatId, float offset) {
        getChatConfig(chatId).setGmtOffset(offset);
    }

    public ChatConfig save(ChatConfig chatConfig) {
        return chatConfigRepository.save(chatConfig);
    }

    public ResponseMsgDataDTO createNotificationsMsg(Long chatId, UtilsBot bot) {
        ChatConfig chatConfig = getChatConfig(chatId);
        Set<UserData> allUserData = chatConfig.getUserData();
        String responseMsg = String.format(EVERYONE_CONFIG.getValue(),
                String.join(",", allUserData.stream().map(bot::getUsername).toList())
        );
        List<InlineKeyboardButton> buttons = chatConfig.getNotifications()
                .stream()
                .map(notification -> createBtn(notification.getScheduledFor().format(defaultDTFormatter), "NF_ID_" + notification.getId()))
                .collect(Collectors.toList());
        if (buttons.size() < appProperties.getBot().getNotificationsLimit()) {
            buttons.add(createBtn("+ New notification", ADD_NOTIFICATION));
        }
        return new ResponseMsgDataDTO(
                responseMsg,
                notificationConfig(buttons)
        );
    }
}
