package com.organization.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.organization.pojo.ChatMessage;
import com.organization.service.SaveMessageService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class SaveMessageServiceImpl implements SaveMessageService {
    // 全局复用ObjectMapper，支持LocalDateTime序列化
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void saveChatMessage(ChatMessage chatMessage) {
        File file = new File("Chat/" + chatMessage.getChatId()+".json");
        System.out.println("保存到" +"Chat/" +  chatMessage.getChatId()+".json");
        JSONArray chatArray = new JSONArray();

        chatMessage.setSendTime(LocalDateTime.now());
        try {
            // 读取文件
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                    String fileContent = reader.lines().collect(Collectors.joining());
                    if (!fileContent.isEmpty()) {
                        try {
                            //尝试解析为JSONArray
                            chatArray = new JSONArray(fileContent);
                        } catch (org.json.JSONException e1) {
                            try {
                                // 解析失败，尝试解析为单个JSONObject
                                JSONObject oldMsg = new JSONObject(fileContent);
                                chatArray.put(oldMsg); // 包装成数组
                                System.out.println("检测到旧消息格式（单个JSON对象），自动迁移为数组格式");
                            } catch (org.json.JSONException e2) {
                                //为空
                                System.err.println("历史消息格式无效，将创建新消息列表");
                                e2.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("读取历史消息失败，将创建新消息列表");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在，创建文件
                file.createNewFile();
            }

            //添加当前聊天记录
            String newMsgJson = OBJECT_MAPPER.writeValueAsString(chatMessage);
            chatArray.put(new JSONObject(newMsgJson));

            //写入文件
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(chatArray.toString(4));
            }

        } catch (Exception e) {
            System.err.println("保存聊天消息失败");
            e.printStackTrace();
        }
    }
}