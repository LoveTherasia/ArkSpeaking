package com.organization.service;

import com.organization.pojo.ChatMessage;

import java.io.IOException;
import java.util.List;

public interface ReadMessageService {
    //读取信息
    public List<ChatMessage> readChatMessage(String characterId) throws IOException;
}
