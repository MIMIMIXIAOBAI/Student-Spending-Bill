package com.campusbill.app.chat.ai.assistant;

import org.json.JSONArray;
import org.json.JSONObject;

/** think-protocol.v0.1.md 的 JSONL 事件。每条都过一遍密钥擦除。 */
public final class ProcessEvents {

    private ProcessEvents() {
    }

    public static String status(String ts, String value) {
        return line(new JSONObject().put("ts", ts).put("type", "status").put("value", value));
    }

    public static String prompt(String ts, String version, String hash) {
        return line(new JSONObject()
                .put("ts", ts)
                .put("type", "prompt")
                .put("version", version)
                .put("hash", hash));
    }

    public static String think(String ts, String text) {
        return line(new JSONObject().put("ts", ts).put("type", "think").put("text", text));
    }

    public static String toolCall(String ts, String name, String arguments) {
        JSONObject event = new JSONObject()
                .put("ts", ts)
                .put("type", "tool_call")
                .put("name", name);
        String redacted = SecretRedactor.redact(arguments);
        try {
            event.put("arguments", new JSONObject(redacted));
        } catch (Exception e) {
            event.put("arguments", redacted);
        }
        return line(event);
    }

    public static String toolResult(String ts, String name, String summary) {
        return line(new JSONObject()
                .put("ts", ts)
                .put("type", "tool_result")
                .put("name", name)
                .put("result_summary", summary));
    }

    public static String confirmRecord(String ts, String action, boolean accepted,
                                       JSONObject preview, JSONObject verified) {
        return line(new JSONObject()
                .put("ts", ts)
                .put("type", "confirm_record")
                .put("action", action)
                .put("accepted", accepted)
                .put("preview", preview == null ? new JSONObject() : preview)
                .put("verified", verified == null ? new JSONObject() : verified));
    }

    public static String decision(String ts, String text, JSONArray planned) {
        return line(new JSONObject()
                .put("ts", ts)
                .put("type", "decision")
                .put("text", text)
                .put("planned", planned == null ? new JSONArray() : planned));
    }

    private static String line(JSONObject event) {
        return SecretRedactor.redact(event.toString());
    }
}
