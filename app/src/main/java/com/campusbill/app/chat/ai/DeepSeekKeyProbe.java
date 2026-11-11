package com.campusbill.app.chat.ai;

import com.campusbill.app.chat.ai.assistant.AssistantCallException;
import com.campusbill.app.chat.ai.assistant.FailureClassifier;
import com.campusbill.app.chat.ai.assistant.FailureKind;
import com.campusbill.app.chat.ai.assistant.SecretRedactor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 用一次最短的 chat completion 检查 DeepSeek 密钥。不把密钥写进异常信息。 */
public final class DeepSeekKeyProbe {

    private DeepSeekKeyProbe() {
    }

    public static void check(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AssistantCallException(FailureKind.API_KEY, "请先粘贴 DeepSeek API Key。");
        }
        HttpURLConnection connection = null;
        try {
            JSONObject body = new JSONObject();
            body.put("model", AiConfig.DEFAULT_MODEL);
            body.put("max_tokens", 1);
            body.put("stream", false);
            body.put("messages", new JSONArray().put(new JSONObject()
                    .put("role", "user")
                    .put("content", "ping")));
            connection = (HttpURLConnection) new URL(AiConfig.DEFAULT_BASE_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = SecretRedactor.redact(readLimited(stream));
            if (code < 200 || code >= 300) {
                FailureKind kind = FailureClassifier.classifyHttp(code, response);
                throw new AssistantCallException(kind, FailureClassifier.message(kind));
            }
        } catch (AssistantCallException e) {
            throw e;
        } catch (Exception e) {
            throw new AssistantCallException(FailureKind.NETWORK, FailureClassifier.message(FailureKind.NETWORK));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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
