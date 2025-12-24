package com.organization.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.organization.pojo.ChatMessage;
import com.organization.service.AIChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIChatServiceImpl implements AIChatService {
    // 1. 直接指定虚拟环境的Python解释器路径（核心）
    private static final String PYTHON_EXEC_PATH = "E:\\ArkSpeaking\\backend\\.venv\\Scripts\\python.exe";
    private static final Logger log = LoggerFactory.getLogger(AIChatServiceImpl.class);
    private static final String PYTHON_SCRIPT_PATH = "E:\\ArkSpeaking\\backend\\src\\main\\resources\\python\\AIChat.py";


    @Override
    public ChatMessage generateReply(ChatMessage userMessage) throws IOException, InterruptedException {
        //构建回复信息
        ChatMessage reply = new ChatMessage();

        // 步骤1：非空校验
        if (userMessage == null) {
            throw new IllegalArgumentException("用户消息不能为空");
        }
        String chatId = userMessage.getChatId();
        String content = userMessage.getContent();
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException("ChatId不能为空或空白");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空或空白");
        }

        // 步骤2：校验Python脚本文件是否存在
        File scriptFile = new File(PYTHON_SCRIPT_PATH);
        if (!scriptFile.exists()) {
            throw new IOException("Python脚本文件不存在：" + PYTHON_SCRIPT_PATH);
        }
        if (!scriptFile.isFile()) {
            throw new IOException("指定路径不是文件：" + PYTHON_SCRIPT_PATH);
        }

        // 步骤3：构建正确的命令列表（仅保留：Python解释器 + 脚本 + 参数）
        List<String> command = new ArrayList<>();
        command.add(PYTHON_EXEC_PATH);       // 唯一的Python解释器入口
        command.add(PYTHON_SCRIPT_PATH);     // 脚本路径
        command.add(chatId);                 // 参数1：角色ID
        command.add(content);                // 参数2：聊天内容

        log.info("执行Python脚本，命令：{}", command);

        // 步骤4：执行脚本（优化异常处理+编码）
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // 合并错误流和输出流
        // 显式指定环境编码（避免中文乱码）
        processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");
        Process process = null;
        try {
            process = processBuilder.start();

            // 读取脚本输出（强制UTF-8编码）
            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            // 等待脚本执行完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python脚本执行失败，退出码：{}，输出：{}", exitCode, output);
                throw new RuntimeException("Python脚本执行失败，退出码：" + exitCode + "，输出：" + output);
            }

            log.info("Python脚本执行成功，输出：{}", output.toString().trim());

            String json_content = output.toString();
            JSONObject jsonObject = JSONObject.parseObject(json_content);
            String aiResponse = jsonObject.getString("ai_response");
            int currentFavor =  jsonObject.getInteger("current_favor");

            reply.setSendId(chatId);
            reply.setChatId(chatId);
            reply.setContent(aiResponse);
            reply.setFavor(currentFavor);

            return reply;
        } finally {
            // 确保进程销毁，避免资源泄漏
            if (process != null) {
                process.destroy();
            }
        }
    }
}