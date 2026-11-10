package com.campusbill.app.ui;

/**
 * 对话页情绪球面板高度。展开约占窗口三分之一；键盘或确认卡时先收到 160dp，
 * 只有再高就会盖住输入栏时才继续缩小。首页大球不走这里。
 */
public final class ChatBallMetrics {

    public static final int COMPRESSED_DP = 160;
    /** 球面板上下外边距之外再留的空隙，避免贴住状态行。 */
    public static final int GAP_DP = 12;

    private ChatBallMetrics() {
    }

    public static int heightPx(int windowPx, int paddingBottomPx, int abovePx, int inputPx,
                               int confirmPx, int tabPx, boolean compress, float density) {
        int room = windowPx - paddingBottomPx - abovePx - inputPx - confirmPx - tabPx
                - Math.round(GAP_DP * density);
        int preferred = compress
                ? Math.round(COMPRESSED_DP * density)
                : Math.round(windowPx / 3f);
        return Math.min(preferred, Math.max(0, room));
    }
}
