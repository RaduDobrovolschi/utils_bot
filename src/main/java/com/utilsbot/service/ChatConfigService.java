package com.utilsbot.service;

import com.utilsbot.domain.ChatConfig;
import com.utilsbot.repository.ChatConfigRepository;
import jakarta.transaction.Transactional;
import net.suuft.libretranslate.Language;
import org.springframework.stereotype.Service;


@Service
@Transactional
public class ChatConfigService {

    private final ChatConfigRepository chatConfigRepository;

    public ChatConfigService(ChatConfigRepository chatConfigRepository) {
        this.chatConfigRepository = chatConfigRepository;
    }

    public ChatConfig getChatConfig(long chatId){
        return chatConfigRepository.findById(chatId).orElseGet(() ->
                chatConfigRepository.save(new ChatConfig(chatId))
        );
    }

    public void setLanguage(long chatId, Language language) {
        getChatConfig(chatId).setTranslationTargetLang(language);
    }

    public ChatConfig save(ChatConfig chatConfig) {
        return chatConfigRepository.save(chatConfig);
    }
}
