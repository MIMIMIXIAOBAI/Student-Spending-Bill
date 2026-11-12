package com.campusbill.app.chat.ai.assistant;

import java.util.Collections;
import java.util.List;

public final class ChatTurn {
    public final String role;
    public final String content;
    public final List<ToolCall> toolCalls;
    public final String toolCallId;

    private ChatTurn(String role, String content, List<ToolCall> toolCalls, String toolCallId) {
        this.role = role;
        this.content = content == null ? "" : content;
        this.toolCalls = toolCalls == null ? Collections.emptyList() : toolCalls;
        this.toolCallId = toolCallId;
    }

    public static ChatTurn system(String text) {
        return new ChatTurn("system", text, null, null);
    }

    public static ChatTurn user(String text) {
        return new ChatTurn("user", text, null, null);
    }

    public static ChatTurn assistant(String text) {
        return new ChatTurn("assistant", text, null, null);
    }

    public static ChatTurn assistantTools(List<ToolCall> calls) {
        return new ChatTurn("assistant", "", calls, null);
    }

    public static ChatTurn tool(String toolCallId, String content) {
        return new ChatTurn("tool", content, null, toolCallId);
    }
}
