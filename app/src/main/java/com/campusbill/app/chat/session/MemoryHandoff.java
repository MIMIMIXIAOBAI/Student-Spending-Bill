package com.campusbill.app.chat.session;

/**
 * 约 80% 只预存，不切换。约 95% 或本轮放不下才开下一场。
 * 80% 预存不另开一轮「思考」；95% 时若还没有要点，才先抽出再切换。
 */
public final class MemoryHandoff {

    public enum Step {
        NONE,
        PRESTORE,
        AUTO_SWITCH,
        EXTRACT_THEN_SWITCH
    }

    private MemoryHandoff() {
    }

    public static Step afterReply(int percent, boolean memoryReady) {
        if (percent >= ContextUsage.FULL_PERCENT) {
            return memoryReady ? Step.AUTO_SWITCH : Step.EXTRACT_THEN_SWITCH;
        }
        if (percent >= ContextUsage.HINT_PERCENT) {
            return Step.PRESTORE;
        }
        return Step.NONE;
    }

    public static Step beforeSend(int occupiedTokens, int addedTokens, int windowTokens,
                                  boolean memoryReady) {
        if (!ContextUsage.wouldNotFit(occupiedTokens, addedTokens, windowTokens)
                && ContextUsage.percent(occupiedTokens, windowTokens) < ContextUsage.FULL_PERCENT) {
            return Step.NONE;
        }
        return memoryReady ? Step.AUTO_SWITCH : Step.EXTRACT_THEN_SWITCH;
    }
}
