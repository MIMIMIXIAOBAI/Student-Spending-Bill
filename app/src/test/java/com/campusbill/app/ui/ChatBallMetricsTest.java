package com.campusbill.app.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatBallMetricsTest {

    private static final float DENSITY = 3f;

    @Test
    public void 展开时约占窗口三分之一() {
        int window = 2400;
        int height = ChatBallMetrics.heightPx(window, 0, 288, 504, 0, 168, false, DENSITY);
        assertEquals(Math.round(window / 3f), height);
        assertTrue(height > dp(ChatBallMetrics.COMPRESSED_DP));
    }

    @Test
    public void 键盘或确认卡先压到160dp() {
        int window = 2400;
        int keyboard = ChatBallMetrics.heightPx(window, 900, 288, 504, 0, 0, true, DENSITY);
        int confirm = ChatBallMetrics.heightPx(window, 0, 288, 504, 360, 168, true, DENSITY);
        assertEquals(dp(160), keyboard);
        assertEquals(dp(160), confirm);
    }

    @Test
    public void 可用空间低于160dp时仍停在160dp() {
        int window = 1800;
        int compressed = ChatBallMetrics.heightPx(window, 700, 288, 504, 0, 0, true, DENSITY);
        int expanded = ChatBallMetrics.heightPx(window, 700, 288, 504, 0, 0, false, DENSITY);
        int room = 1800 - 700 - 288 - 504 - dp(ChatBallMetrics.GAP_DP);
        assertTrue(room < dp(160));
        assertEquals(dp(160), compressed);
        assertEquals(dp(160), expanded);
    }

    @Test
    public void 三分之一放不下但空间仍够160dp时缩到可用空间() {
        int window = 2400;
        int height = ChatBallMetrics.heightPx(window, 0, 400, 900, 200, 200, false, DENSITY);
        int room = 2400 - 400 - 900 - 200 - 200 - dp(ChatBallMetrics.GAP_DP);
        assertTrue(Math.round(window / 3f) > room);
        assertTrue(room >= dp(160));
        assertEquals(room, height);
    }

    private static int dp(int value) {
        return Math.round(value * DENSITY);
    }
}
