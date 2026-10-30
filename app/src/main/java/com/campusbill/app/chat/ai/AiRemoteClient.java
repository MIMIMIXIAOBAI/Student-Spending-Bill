package com.campusbill.app.chat.ai;

import android.os.Handler;
import android.os.Looper;

import com.campusbill.app.chat.ai.presets.AiPresets;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 远程 AI 客户端：仅在 AI 对话页、用户已配置 API KEY 时联网请求。
 * 使用 OpenAI 兼容的 /chat/completions 协议，后台线程发起请求，结果回主线程。
 */
public final class AiRemoteClient {

    public interface Callback {
        void onReply(String reply);

        void onFailure(Exception e);
    }

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 20000;
    /** 最多携带的历史对话条数（不含本次提问） */
    private static final int MAX_HISTORY = 6;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private AiRemoteClient() {
    }

    /**
     * 发起对话请求。
     *
     * @param history     历史消息，元素为 [role, content]，role 为 user / assistant
     * @param billContext 账单快照文本（由 BillContextBuilder 生成），拼入 system prompt，
     *                    让远程 AI 基于用户真实账单作答；无数据或读取失败时传 null
     */
    public static void requestReply(String apiKey, String baseUrl, String model,
                                    List<String[]> history, String question,
                                    String billContext, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                String reply = doRequest(apiKey, baseUrl, model, history, question, billContext);
                MAIN_HANDLER.post(() -> callback.onReply(reply));
            } catch (Exception e) {
                MAIN_HANDLER.post(() -> callback.onFailure(e));
            }
        });
    }

    private static String doRequest(String apiKey, String baseUrl, String model,
                                    List<String[]> history, String question,
                                    String billContext) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", 0.7);

        String systemPrompt = AiPresets.SYSTEM_PROMPT;
        if (billContext != null && !billContext.trim().isEmpty()) {
            systemPrompt = systemPrompt + "\n\n" + billContext.trim();
        }

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", systemPrompt));
        if (history != null) {
            int start = Math.max(0, history.size() - MAX_HISTORY);
            for (int i = start; i < history.size(); i++) {
                String[] item = history.get(i);
                messages.put(new JSONObject()
                        .put("role", item[0])
                        .put("content", item[1]));
            }
        }
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", question));
        body.put("messages", messages);
        body.put("stream", false);

        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = readAll(stream);
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code + " " + response);
            }
            return parseReply(response);
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    /** 解析 OpenAI 兼容响应中的回复正文 */
    private static String parseReply(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new IllegalStateException("响应中没有 choices");
        }
        JSONObject message = choices.getJSONObject(0).optJSONObject("message");
        String content = message == null ? null : message.optString("content", "");
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalStateException("响应中没有回复内容");
        }
        return content.trim();
    }
}
