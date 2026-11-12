package com.campusbill.app.chat.ai.assistant;

/**
 * 涉及账单的这一轮，在 query_spend 成功之前，不把带金额或笔数的文字交给用户。
 */
public final class ReplyGate {

    private ReplyGate() {
    }

    public static boolean withhold(boolean needsLedger, boolean queried, String text) {
        return needsLedger && !queried && hasFigure(text);
    }

    static boolean hasFigure(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        if (text.indexOf('¥') >= 0 || text.indexOf('元') >= 0) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
