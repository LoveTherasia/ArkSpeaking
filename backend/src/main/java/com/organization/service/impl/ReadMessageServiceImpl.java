package com.organization.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.organization.pojo.ChatMessage;
import com.organization.service.ReadMessageService;
import com.organization.utils.JsonFileReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReadMessageServiceImpl implements ReadMessageService{
    private final JsonFileReader jsonFileReader = new JsonFileReader();

    public List<ChatMessage> readChatMessage(String characterId) throws IOException {
        System.out.println(characterId);
        return jsonFileReader.readMessages("Chat/" + characterId + ".json");
    }
}
