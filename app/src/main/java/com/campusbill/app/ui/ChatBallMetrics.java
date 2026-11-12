package com.campusbill.app.ui;

/**
 * 对话页情绪球面板高度。展开约占窗口三分之一；键盘或确认卡时压到 160dp。
 * 160dp 是硬下限，返回值不会再低。首页大球不走这里。
 */
public final class ChatBallMetrics {

    public static final int COMPRESSED_DP = 160;
    /** 球面板上下外边距之外再留的空隙，避免贴住状态行。 */
    public static final int GAP_DP = 12;

    private ChatBallMetrics() {
    }

    public static int heightPx(int windowPx, int paddingBottomPx, int abovePx, int inputPx,
                               int confirmPx, int tabPx, boolean compress, float density) {
        int floor = Math.round(COMPRESSED_DP * density);
        int room = windowPx - paddingBottomPx - abovePx - inputPx - confirmPx - tabPx
                - Math.round(GAP_DP * density);
        int preferred = compress ? floor : Math.max(floor, Math.round(windowPx / 3f));
        if (room >= floor) {
            return Math.min(preferred, room);
        }
        return floor;
    }
}
