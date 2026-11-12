package com.campusbill.app.chat.ai.assistant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class StreamResult {
    public final String think;
    public final String content;
    public final List<ToolCall> calls;
    public final FailureKind failure;
    public final int promptTokens;
    public final int completionTokens;

    private StreamResult(String think, String content, List<ToolCall> calls, FailureKind failure,
                         int promptTokens, int completionTokens) {
        this.think = think == null ? "" : think;
        this.content = content == null ? "" : content;
        this.calls = calls == null ? Collections.emptyList() : calls;
        this.failure = failure;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    public static StreamResult of(String think, String content, List<ToolCall> calls) {
        return of(think, content, calls, 0, 0);
    }

    public static StreamResult of(String think, String content, List<ToolCall> calls,
                                  int promptTokens, int completionTokens) {
        return new StreamResult(think, content, calls, null, promptTokens, completionTokens);
    }

    public static StreamResult text(String content) {
        return new StreamResult("", content, Collections.emptyList(), null, 0, 0);
    }

    public static StreamResult tools(ToolCall... calls) {
        return new StreamResult("", "", Arrays.asList(calls), null, 0, 0);
    }

    public static StreamResult fail(FailureKind kind, String message) {
        return new StreamResult("", message, Collections.emptyList(), kind, 0, 0);
    }
}
