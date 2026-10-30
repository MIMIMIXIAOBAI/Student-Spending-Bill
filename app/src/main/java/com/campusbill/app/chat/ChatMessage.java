package com.campusbill.app.chat;

/** 一条对话消息：区分用户消息与 AI 消息 */
public class ChatMessage {

    /** AI 消息（左侧气泡） */
    public static final int TYPE_AI = 0;
    /** 用户消息（右侧气泡） */
    public static final int TYPE_USER = 1;

    public final int type;
    public final String content;

    public ChatMessage(int type, String content) {
        this.type = type;
        this.content = content;
    }
}
