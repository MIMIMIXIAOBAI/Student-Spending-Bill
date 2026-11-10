package com.campusbill.app.chat.ai.assistant;

/** 会把状态行设为「出错」的失败。账本读写失败不在这里。 */
public enum FailureKind {
    API_KEY,
    QUOTA,
    NETWORK
}
