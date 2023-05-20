package com.utilsbot.service;

import com.utilsbot.domain.ChatConfig;
import com.utilsbot.domain.UserData;
import com.utilsbot.domain.UserDataIds;
import com.utilsbot.repository.UserDataRepository;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class UserDataService {

    private final UserDataRepository userDataRepository;
    private final ChatConfigService chatConfigService;

    public UserDataService(UserDataRepository userDataRepository, ChatConfigService chatConfigService) {
        this.userDataRepository = userDataRepository;
        this.chatConfigService = chatConfigService;
    }

    public Set<UserData> getAllUserData(Long chatId) {
        Set<UserData> userData = chatConfigService.getChatConfig(chatId).getUserData();
        Hibernate.initialize(userData);
        return userData;
    }

    public void addPrivateChatUser(Long chatId, Long userId) {
        ChatConfig chatConfig = chatConfigService.getChatConfig(chatId);
        if (chatConfig.getUserData().isEmpty()) {
            userDataRepository.save(
                    new UserData(
                            userId,
                            chatConfig
                    )
            );
        }
    }

    public void deleteUser(Long chatId, Long userId) {
        ChatConfig chatConfig = chatConfigService.getChatConfig(chatId);
        userDataRepository.deleteById(new UserDataIds(
                userId, chatConfig
        ));
    }

    public Optional<UserData> handleCommand(Long chatId, Long userId) {
        ChatConfig chatConfig = chatConfigService.getChatConfig(chatId);
        Optional<UserData> byId = userDataRepository.findById(new UserDataIds(userId, chatConfig));
        if (byId.isPresent()) {
            userDataRepository.delete(byId.get());
            return Optional.empty();
        } else {
            UserData save = userDataRepository.save(
                    new UserData(
                            userId,
                            chatConfig
                    )
            );
            return Optional.of(save);
        }
    }
}
