package com.organization.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

//聊天记录类
@Data
public class ChatMessage {
    //发送者ID,用于辨别是用户发送的消息还是AI发送的消息
    private String sendId;
    //聊天对象ID,根据这个来将聊天记录存储在不同位置
    private String chatId;
    //聊天内容
    private String content;
    //聊天时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private LocalDateTime sendTime;
}
