package com.campusbill.app.chat.ai.assistant;

/** 过程日志里不许留下密钥。 */
public final class SecretRedactor {

    private SecretRedactor() {
    }

    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text
                .replaceAll("sk-[A-Za-z0-9_\\-]+", "sk-***")
                .replaceAll("(?i)bearer\\s+[A-Za-z0-9\\-._~+/]+=*", "Bearer ***");
    }
}
