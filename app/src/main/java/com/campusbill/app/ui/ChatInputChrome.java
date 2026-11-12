package com.campusbill.app.ui;

/**
 * 对话页底部输入区的冻结高度。空闲不预留状态行和上下文百分比。
 */
public final class ChatInputChrome {

    public static final int INPUT_PAD_VERTICAL_DP = 8;
    public static final int INPUT_PAD_HORIZONTAL_DP = 16;
    public static final int FIELD_MIN_DP = 40;
    public static final int FIELD_MAX_DP = 96;
    public static final int STATUS_DP = 32;
    public static final int CONTEXT_PAD_VERTICAL_DP = 4;
    public static final int LIST_TOP_DP = 8;

    private ChatInputChrome() {
    }

    /** 单行 40dp；字满约 3 行时停在 96dp，框内滚动。 */
    public static int fieldDp(boolean grownFull) {
        return grownFull ? FIELD_MAX_DP : FIELD_MIN_DP;
    }

    /** 上下各 8dp，加上输入框。单行整行 56dp，长满时为 112dp。 */
    public static int inputRowDp(boolean grownFull) {
        return INPUT_PAD_VERTICAL_DP + fieldDp(grownFull) + INPUT_PAD_VERTICAL_DP;
    }

    public static boolean reserveStatus(String text) {
        if (text == null) {
            return false;
        }
        switch (text) {
            case "思考":
            case "调用工具":
            case "等待确认":
            case "出错":
            case "密钥失败":
            case "余额不足":
            case "网络错误":
                return true;
            default:
                return false;
        }
    }

    public static boolean reserveContext(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
