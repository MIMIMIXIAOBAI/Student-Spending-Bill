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
        String awaiting = "35";
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

    /** 结果态，不新增文字状态。 */
    public String idForScene(String scene) {
        if ("pleased".equals(scene)) {
            return "19";
        }
        if ("light".equals(scene)) {
            return "10";
        }
        if ("concerned".equals(scene)) {
            return error;
        }
        return "02";
    }
}
