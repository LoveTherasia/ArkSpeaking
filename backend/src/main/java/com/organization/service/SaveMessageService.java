package com.organization.service;

import com.organization.pojo.ChatMessage;

public interface SaveMessageService {
    //保存信息
    public void saveChatMessage(ChatMessage chatMessage);
}
