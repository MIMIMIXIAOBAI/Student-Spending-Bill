package com.campusbill.app.chat.voice;

import org.json.JSONObject;

/** 从 Vosk 的 result / partial JSON 里取出文本。 */
public final class VoskResultText {

    private VoskResultText() {
    }

    public static String text(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "";
        }
        try {
            JSONObject object = new JSONObject(json);
            String partial = object.optString("partial", "").trim();
            if (!partial.isEmpty()) {
                return partial;
            }
            return object.optString("text", "").trim();
        } catch (Exception e) {
            return "";
        }
    }
}
