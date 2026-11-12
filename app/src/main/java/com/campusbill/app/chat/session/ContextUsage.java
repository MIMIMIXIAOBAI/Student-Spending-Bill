package com.campusbill.app.chat.session;

/** 上下文百分比只用 token 和窗口，不用消息条数。 */
public final class ContextUsage {

    public static final int HINT_PERCENT = 80;
    public static final int FULL_PERCENT = 95;

    public enum Level {
        NORMAL,
        HINT,
        FULL
    }

    private ContextUsage() {
    }

    public static int percent(int occupiedTokens, int windowTokens) {
        if (windowTokens <= 0 || occupiedTokens <= 0) {
            return 0;
        }
        long value = Math.round(occupiedTokens * 100.0 / windowTokens);
        if (value > 100) {
            return 100;
        }
        return (int) value;
    }

    public static Level level(int percent) {
        if (percent >= FULL_PERCENT) {
            return Level.FULL;
        }
        if (percent >= HINT_PERCENT) {
            return Level.HINT;
        }
        return Level.NORMAL;
    }

    /** 下一轮请求放不进当前窗口，或已经到约 95%。 */
    public static boolean wouldNotFit(int occupiedTokens, int addedTokens, int windowTokens) {
        if (windowTokens <= 0) {
            return false;
        }
        int next = occupiedTokens + Math.max(0, addedTokens);
        return next > windowTokens || percent(next, windowTokens) >= FULL_PERCENT;
    }
}
