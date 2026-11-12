package com.campusbill.app.chat.ai.assistant;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 运行时加载点。源文件在 docs/assistant/，构建时同步到 assets/assistant/。
 * 文件名与 DSH v0.1 一致，这里不另写一套 schema。
 */
public final class AssistantDocuments implements AssistantController.Docs {

    public static final String PROMPT = "SYSTEM_PROMPT.v0.1.md";
    public static final String SCHEMA = "tools.schema.v0.1.json";
    public static final String INTENT = "intent-control.v0.1.md";
    public static final String THINK = "think-protocol.v0.1.md";
    public static final String EMOTION = "emotion-map.v0.1.md";

    private final String promptBody;
    private final String toolsArray;
    private final String version;
    private final String emotionMap;

    private AssistantDocuments(String promptBody, String toolsArray, String version, String emotionMap) {
        this.promptBody = promptBody;
        this.toolsArray = toolsArray;
        this.version = version;
        this.emotionMap = emotionMap;
    }

    public static AssistantDocuments load(Context context) throws Exception {
        String promptFile = readAsset(context, PROMPT);
        String schema = readAsset(context, SCHEMA);
        return new AssistantDocuments(
                SystemPromptBody.extract(promptFile),
                ToolSchemaCatalog.toolsArray(schema),
                ToolSchemaCatalog.version(schema),
                readAsset(context, EMOTION));
    }

    /** 协议文件留给界面和后续版本读取，不在这里改写。 */
    public static String readProtocol(Context context, String fileName) throws Exception {
        return readAsset(context, fileName);
    }

    private static String readAsset(Context context, String name) throws Exception {
        try (InputStream input = context.getAssets().open("assistant/" + name)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Override
    public String systemPromptBody() {
        return promptBody;
    }

    @Override
    public String toolsArray() {
        return toolsArray;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public String emotionMap() {
        return emotionMap;
    }
}
