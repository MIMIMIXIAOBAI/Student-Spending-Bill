package com.campusbill.app.chat.ai.assistant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** think-protocol.v0.1.md 的 JSONL 事件。每条都过一遍密钥擦除。 */
public final class ProcessEvents {

    private ProcessEvents() {
    }

    public static String status(String ts, String value) {
        try {
            return line(new JSONObject().put("ts", ts).put("type", "status").put("value", value));
        } catch (JSONException e) {
            return "{}";
        }
    }

    public static String prompt(String ts, String version, String hash) {
        try {
            return line(new JSONObject()
                    .put("ts", ts)
                    .put("type", "prompt")
                    .put("version", version)
                    .put("hash", hash));
        } catch (JSONException e) {
            return "{}";
        }
    }

    public static String think(String ts, String text) {
        try {
            return line(new JSONObject().put("ts", ts).put("type", "think").put("text", text));
        } catch (JSONException e) {
            return "{}";
        }
    }

    public static String toolCall(String ts, String name, String arguments) {
        try {
            JSONObject event = new JSONObject()
                    .put("ts", ts)
                    .put("type", "tool_call")
                    .put("name", name);
            String redacted = SecretRedactor.redact(arguments);
            try {
                event.put("arguments", new JSONObject(redacted));
            } catch (JSONException e) {
                event.put("arguments", redacted);
            }
            return line(event);
        } catch (JSONException e) {
            return "{}";
        }
    }

    public static String toolResult(String ts, String name, String summary) {
        try {
            return line(new JSONObject()
                    .put("ts", ts)
                    .put("type", "tool_result")
                    .put("name", name)
                    .put("result_summary", summary));
        } catch (JSONException e) {
            return "{}";
        }
    }

    public static String confirmRecord(String ts, String action, boolean accepted,
                                       JSONObject preview, JSONObject verified) {
        try {
            return line(new JSONObject()
                    .put("ts", ts)
                    .put("type", "confirm_record")
                    .put("action", action)
                    .put("accepted", accepted)
                    .put("preview", preview == null ? new JSONObject() : preview)
                    .put("verified", verified == null ? new JSONObject() : verified));
        } catch (JSONException e) {
            return "{}";
        }
    }

    public static String decision(String ts, String text, JSONArray planned) {
        try {
            return line(new JSONObject()
                    .put("ts", ts)
                    .put("type", "decision")
                    .put("text", text)
                    .put("planned", planned == null ? new JSONArray() : planned));
        } catch (JSONException e) {
            return "{}";
        }
    }

    private static String line(JSONObject event) {
        return SecretRedactor.redact(event.toString());
    }
}
