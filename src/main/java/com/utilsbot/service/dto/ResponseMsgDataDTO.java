package com.utilsbot.service.dto;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public record ResponseMsgDataDTO(
        String responseMsg,
        InlineKeyboardMarkup replyMarkup
) {
    public ResponseMsgDataDTO(String responseMsg) {
        this(responseMsg, null);
    }

    public ResponseMsgDataDTO(InlineKeyboardMarkup replyMarkup) {
        this(null, replyMarkup);
    }
}
