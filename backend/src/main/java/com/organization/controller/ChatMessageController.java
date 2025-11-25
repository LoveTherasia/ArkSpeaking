package com.organization.controller;

import com.organization.pojo.ChatMessage;
import com.organization.service.ReadMessageService;
import com.organization.service.SaveMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatMessageController {

    @Autowired
    private SaveMessageService saveMessageService;

    @Autowired
    private ReadMessageService readMessageService;

    @PostMapping("/save")
    public void saveChatMessage(@RequestBody ChatMessage chatMessage) {
        //这里可能应该要加上参数检验

        //调用Service层方法
        saveMessageService.saveChatMessage(chatMessage);
    }

    @GetMapping("/read")
    public List<ChatMessage> readChatMessage(String characterId) throws IOException {
        //这里应该加上参数检验

        //调用Service层方法
        List<ChatMessage> chatMessages = readMessageService.readChatMessage(characterId);
        for (ChatMessage chatMessage : chatMessages) {
            System.out.println(chatMessage);
        }
        readMessageService.readChatMessage(characterId);
        return chatMessages;
    }
}
