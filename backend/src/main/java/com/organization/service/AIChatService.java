package com.organization.service;

import com.organization.pojo.ChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public interface AIChatService {
    public ChatMessage generateReply(ChatMessage userMessage) throws IOException, InterruptedException;
}
