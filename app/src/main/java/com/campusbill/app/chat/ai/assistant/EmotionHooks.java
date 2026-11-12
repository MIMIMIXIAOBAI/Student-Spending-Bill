package com.campusbill.app.chat.ai.assistant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 球的表情由客户端映射。默认对齐 emotion-map.v0.1.md。
 * 协议文件以后若写上 {@code hook:出错=38} 这类行，这里会接住，不必改状态文案。
 */
public final class EmotionHooks {

    private static final Pattern HOOK = Pattern.compile("hook:(思考|调用工具|等待确认|出错)=(\\d+)");

    private final String thinking;
    private final String tool;
    private final String awaiting;
    private final String error;

    private EmotionHooks(String thinking, String tool, String awaiting, String error) {
        this.thinking = thinking;
        this.tool = tool;
        this.awaiting = awaiting;
        this.error = error;
    }

    public static EmotionHooks load(String emotionMapMarkdown) {
        String thinking = "30";
        String tool = "32";
        String awaiting = "03";
        String error = "34";
        if (emotionMapMarkdown != null) {
            Matcher matcher = HOOK.matcher(emotionMapMarkdown);
            while (matcher.find()) {
                String id = matcher.group(2);
                switch (matcher.group(1)) {
                    case "思考":
                        thinking = id;
                        break;
                    case "调用工具":
                        tool = id;
                        break;
                    case "等待确认":
                        awaiting = id;
                        break;
                    case "出错":
                        error = id;
                        break;
                    default:
                        break;
                }
            }
        }
        return new EmotionHooks(thinking, tool, awaiting, error);
    }

    public String idFor(AssistantStatus status) {
        if (status == null) {
            return "02";
        }
        switch (status) {
            case THINKING:
                return thinking;
            case TOOL:
                return tool;
            case AWAITING_CONFIRM:
                return awaiting;
            case ERROR:
                return error;
            default:
                return "02";
        }
    }

    /**
     * 只返回 aora-bot 两位 id。未知名字回 02，避免把英文状态名交给 setEmotion。
     * listening=35 等待输入，thinking=30 思考中，streaming=39 输出回复，
     * pleased=33 任务完成，number=19 满意，query_tool=40 检索资料，idle=02。
     * 等待确认走 {@link #idFor} 的 03，不复用 35。
     */
    public String idForScene(String scene) {
        if ("listening".equals(scene)) {
            return "35";
        }
        if ("thinking".equals(scene)) {
            return thinking;
        }
        if ("streaming".equals(scene)) {
            return "39";
        }
        if ("tool".equals(scene)) {
            return tool;
        }
        if ("query_tool".equals(scene)) {
            return "40";
        }
        if ("pleased".equals(scene)) {
            return "33";
        }
        if ("number".equals(scene)) {
            return "19";
        }
        if ("light".equals(scene)) {
            return "10";
        }
        if ("concerned".equals(scene) || "error".equals(scene)) {
            return error;
        }
        return "02";
    }
}
