package com.campusbill.app.chat.ai.assistant;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 读取 SYSTEM_PROMPT.v0.1.md：分隔线之前是作者说明，不发给模型。 */
public final class SystemPromptBody {

    private SystemPromptBody() {
    }

    public static String extract(String fileText) {
        if (fileText == null) {
            return "";
        }
        String[] lines = fileText.split("\\R", -1);
        int marker = -1;
        for (int i = 0; i < lines.length; i++) {
            if ("---".equals(lines[i].trim())) {
                marker = i;
                break;
            }
        }
        if (marker < 0) {
            return fileText.trim();
        }
        StringBuilder body = new StringBuilder();
        for (int i = marker + 1; i < lines.length; i++) {
            if (body.length() > 0) {
                body.append('\n');
            }
            body.append(lines[i]);
        }
        return body.toString().trim();
    }

    public static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
