package com.organization.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.organization.pojo.ChatMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;

public class JsonFileReader {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final String BASE_PATH = Paths.get(".").toAbsolutePath().toString();

    public static List<ChatMessage> readMessages(String path) {
        File file = Paths.get(BASE_PATH, path).toFile();
        //捕获文件不存在异常，返回null
        try {
            try (InputStream inputStream = new FileInputStream(file)) {
                return mapper.readValue(inputStream, new TypeReference<List<ChatMessage>>() {});
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            // 文件不存在时返回null
            return null;
        } catch (IOException e) {
            // 其他IO异常
            throw new RuntimeException(e);
        }
    }
}