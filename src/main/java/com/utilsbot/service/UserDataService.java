package com.utilsbot.service;

import com.utilsbot.domain.UserData;
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
        Hibernate.initialize(userData); //poxyu
        return userData;
    }

    public Optional<UserData> handleCommand(Long chatId, Long userId) {
        Optional<UserData> byId = userDataRepository.findById(userId);
        if (byId.isPresent()) {
            userDataRepository.delete(byId.get());
            return Optional.empty();
        } else {
            return Optional.of(
                    userDataRepository.save(
                            new UserData(
                                    userId,
                                    chatConfigService.getChatConfig(chatId)
                            )
                    )
            );
        }
    }
}
