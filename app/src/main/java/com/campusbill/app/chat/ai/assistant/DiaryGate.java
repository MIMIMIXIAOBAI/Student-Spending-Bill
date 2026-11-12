package com.campusbill.app.chat.ai.assistant;

/** 日记正文只有在用户确认当前这次申请之后才能被引用。 */
public final class DiaryGate {

    private String pendingPurpose;
    private boolean confirmed;

    public void request(String purpose) {
        pendingPurpose = purpose;
        confirmed = false;
    }

    public void confirm() {
        if (pendingPurpose != null) {
            confirmed = true;
        }
    }

    public void cancel() {
        pendingPurpose = null;
        confirmed = false;
    }

    public boolean mayCite() {
        return confirmed;
    }

    public String citationOrEmpty(String diary) {
        if (!confirmed || diary == null) {
            return "";
        }
        return diary;
    }
}
