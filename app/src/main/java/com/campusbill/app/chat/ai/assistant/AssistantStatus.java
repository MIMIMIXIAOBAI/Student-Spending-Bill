package com.campusbill.app.chat.ai.assistant;

/**
 * 文字状态只有四档。空闲时界面留空，不算第五档。
 * 「出错」由项目负责人追加，仅用于密钥、余额/配额和网络失败。
 * think-protocol.v0.1.md 若尚未写入「出错」，仍由 {@link EmotionHooks} 留钩子。
 */
public enum AssistantStatus {
    THINKING("思考"),
    TOOL("调用工具"),
    AWAITING_CONFIRM("等待确认"),
    ERROR("出错");

    public final String label;

    AssistantStatus(String label) {
        this.label = label;
    }
}
