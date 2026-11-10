package com.campusbill.app.chat.ai.assistant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** 累积 OpenAI/DeepSeek 兼容的 SSE data 行。 */
public final class SseAccumulator {

    private final ThinkTagSplitter splitter = new ThinkTagSplitter();
    private final StringBuilder reasoning = new StringBuilder();
    private final TreeMap<Integer, StringBuilder> names = new TreeMap<>();
    private final TreeMap<Integer, StringBuilder> arguments = new TreeMap<>();
    private final TreeMap<Integer, String> ids = new TreeMap<>();
    private int promptTokens;
    private int completionTokens;

    public void acceptLine(String line) {
        if (line == null) {
            return;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("data:")) {
            return;
        }
        String data = trimmed.substring(5).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return;
        }
        try {
            JSONObject root = new JSONObject(data);
            JSONObject usage = root.optJSONObject("usage");
            if (usage != null) {
                promptTokens = usage.optInt("prompt_tokens", promptTokens);
                completionTokens = usage.optInt("completion_tokens", completionTokens);
            }
            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return;
            }
            JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
            if (delta == null) {
                return;
            }
            if (delta.has("content") && !delta.isNull("content")) {
                splitter.push(delta.optString("content", ""));
            }
            if (delta.has("reasoning_content") && !delta.isNull("reasoning_content")) {
                reasoning.append(delta.optString("reasoning_content", ""));
            }
            JSONArray calls = delta.optJSONArray("tool_calls");
            if (calls == null) {
                return;
            }
            for (int i = 0; i < calls.length(); i++) {
                JSONObject call = calls.getJSONObject(i);
                int index = call.optInt("index", i);
                String id = call.optString("id", "");
                if (!id.isEmpty()) {
                    ids.put(index, id);
                }
                JSONObject function = call.optJSONObject("function");
                if (function == null) {
                    continue;
                }
                names.computeIfAbsent(index, key -> new StringBuilder())
                        .append(function.optString("name", ""));
                arguments.computeIfAbsent(index, key -> new StringBuilder())
                        .append(function.optString("arguments", ""));
            }
        } catch (Exception ignored) {
            // 半行或心跳不打断已经累积的内容
        }
    }

    public String think() {
        if (reasoning.length() == 0) {
            return splitter.think();
        }
        if (splitter.think().isEmpty()) {
            return reasoning.toString();
        }
        return reasoning + splitter.think();
    }

    public String content() {
        return splitter.visible();
    }

    public List<ToolCall> toolCalls() {
        List<ToolCall> calls = new ArrayList<>();
        for (Integer index : names.keySet()) {
            String id = ids.getOrDefault(index, "call_" + index);
            calls.add(new ToolCall(id, names.get(index).toString(),
                    arguments.getOrDefault(index, new StringBuilder()).toString()));
        }
        for (Integer index : arguments.keySet()) {
            if (!names.containsKey(index)) {
                calls.add(new ToolCall(ids.getOrDefault(index, "call_" + index),
                        "", arguments.get(index).toString()));
            }
        }
        return calls;
    }

    public int promptTokens() {
        return promptTokens;
    }

    public int completionTokens() {
        return completionTokens;
    }
}
