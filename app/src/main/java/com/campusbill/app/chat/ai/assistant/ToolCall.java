package com.campusbill.app.chat.ai.assistant;

public final class ToolCall {
    public final String id;
    public final String name;
    public final String arguments;

    public ToolCall(String id, String name, String arguments) {
        this.id = id == null || id.isEmpty() ? "call_0" : id;
        this.name = name == null ? "" : name;
        this.arguments = arguments == null || arguments.trim().isEmpty() ? "{}" : arguments;
    }
}
