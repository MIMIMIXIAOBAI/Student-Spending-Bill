package com.campusbill.app.chat.ai.assistant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 只解析 docs/assistant/tools.schema.v0.1.json，不内置第二套工具名。 */
public final class ToolSchemaCatalog {

    private ToolSchemaCatalog() {
    }

    public static String version(String json) {
        return new JSONObject(json).optString("version", "");
    }

    public static String toolsArray(String json) {
        return new JSONObject(json).getJSONArray("tools").toString();
    }

    public static List<String> toolNames(String json) {
        JSONArray tools = new JSONObject(json).getJSONArray("tools");
        List<String> names = new ArrayList<>();
        for (int i = 0; i < tools.length(); i++) {
            names.add(tools.getJSONObject(i).getJSONObject("function").getString("name"));
        }
        return names;
    }
}
