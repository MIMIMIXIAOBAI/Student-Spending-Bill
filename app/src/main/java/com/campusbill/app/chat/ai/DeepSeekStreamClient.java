package com.campusbill.app.chat.ai;

import android.content.Context;

import com.campusbill.app.chat.ai.assistant.AssistantCallException;
import com.campusbill.app.chat.ai.assistant.AssistantController;
import com.campusbill.app.chat.ai.assistant.ChatTurn;
import com.campusbill.app.chat.ai.assistant.FailureClassifier;
import com.campusbill.app.chat.ai.assistant.FailureKind;
import com.campusbill.app.chat.ai.assistant.SecretRedactor;
import com.campusbill.app.chat.ai.assistant.SseAccumulator;
import com.campusbill.app.chat.ai.assistant.StreamResult;
import com.campusbill.app.chat.ai.assistant.ToolCall;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 阻塞式读取 DeepSeek 的 SSE。调用方应在后台线程使用。 */
public final class DeepSeekStreamClient implements AssistantController.Streamer {

    private final Context context;

    public DeepSeekStreamClient(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public StreamResult stream(List<ChatTurn> messages, String toolsArray,
                               AssistantController.Observer observer) {
        String apiKey = AiConfig.getApiKey(context);
        if (apiKey.isEmpty()) {
            throw new AssistantCallException(FailureKind.API_KEY,
                    FailureClassifier.message(FailureKind.API_KEY));
        }
        HttpURLConnection connection = null;
        try {
            JSONObject body = new JSONObject();
            body.put("model", AiConfig.getModel(context));
            body.put("temperature", 0.3);
            body.put("stream", true);
            body.put("messages", messagesJson(messages));
            if (toolsArray != null && !toolsArray.trim().isEmpty() && !"[]".equals(toolsArray.trim())) {
                body.put("tools", new JSONArray(toolsArray));
                body.put("tool_choice", "auto");
            }
            connection = (HttpURLConnection) new URL(AiConfig.getBaseUrl(context)).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept", "text/event-stream");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                String error = SecretRedactor.redact(readLimited(connection.getErrorStream()));
                FailureKind kind = FailureClassifier.classifyHttp(code, error);
                throw new AssistantCallException(kind, FailureClassifier.message(kind));
            }
            SseAccumulator accumulator = new SseAccumulator();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int thinkBefore = accumulator.think().length();
                    int contentBefore = accumulator.content().length();
                    accumulator.acceptLine(line);
                    if (accumulator.think().length() > thinkBefore && observer != null) {
                        observer.onThink(accumulator.think().substring(thinkBefore));
                    }
                    if (accumulator.content().length() > contentBefore && observer != null) {
                        observer.onContent(accumulator.content().substring(contentBefore));
                    }
                }
            }
            return StreamResult.of(accumulator.think(), accumulator.content(), accumulator.toolCalls());
        } catch (AssistantCallException e) {
            throw e;
        } catch (Exception e) {
            throw new AssistantCallException(FailureKind.NETWORK,
                    FailureClassifier.message(FailureKind.NETWORK));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static JSONArray messagesJson(List<ChatTurn> messages) {
        JSONArray array = new JSONArray();
        if (messages == null) {
            return array;
        }
        for (ChatTurn turn : messages) {
            JSONObject item = new JSONObject();
            item.put("role", turn.role);
            if ("tool".equals(turn.role)) {
                item.put("tool_call_id", turn.toolCallId == null ? "" : turn.toolCallId);
                item.put("content", turn.content);
            } else if (turn.toolCalls != null && !turn.toolCalls.isEmpty()) {
                item.put("content", JSONObject.NULL);
                JSONArray calls = new JSONArray();
                for (ToolCall call : turn.toolCalls) {
                    calls.put(new JSONObject()
                            .put("id", call.id)
                            .put("type", "function")
                            .put("function", new JSONObject()
                                    .put("name", call.name)
                                    .put("arguments", call.arguments)));
                }
                item.put("tool_calls", calls);
            } else {
                item.put("content", turn.content);
            }
            array.put(item);
        }
        return array;
    }

    private static String readLimited(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        byte[] buffer = new byte[400];
        int count = stream.read(buffer);
        if (count <= 0) {
            return "";
        }
        return new String(buffer, 0, count, StandardCharsets.UTF_8);
    }
}
