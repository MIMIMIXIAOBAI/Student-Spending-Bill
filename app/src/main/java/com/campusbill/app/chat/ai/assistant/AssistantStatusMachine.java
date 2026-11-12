package com.campusbill.app.chat.ai.assistant;

/** 确认卡是否出现，只由当前状态决定。 */
public final class AssistantStatusMachine {

    private AssistantStatus current;

    public void set(AssistantStatus status) {
        current = status;
    }

    public void clear() {
        current = null;
    }

    public AssistantStatus current() {
        return current;
    }

    public String text() {
        return current == null ? "" : current.label;
    }

    public boolean confirmVisible() {
        return current == AssistantStatus.AWAITING_CONFIRM;
    }
}
