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
    public void 空间不够时继续缩小以免挡住输入栏() {
        int window = 1800;
        int height = ChatBallMetrics.heightPx(window, 700, 288, 504, 0, 0, true, DENSITY);
        int room = 1800 - 700 - 288 - 504 - dp(ChatBallMetrics.GAP_DP);
        assertTrue(room < dp(160));
        assertEquals(room, height);
        assertTrue(height < dp(160));
    }

    @Test
    public void 三分之一放不下时给输入栏和状态行让位() {
        int window = 1200;
        int height = ChatBallMetrics.heightPx(window, 0, 288, 504, 0, 168, false, DENSITY);
        int room = 1200 - 288 - 504 - 168 - dp(ChatBallMetrics.GAP_DP);
        assertTrue(Math.round(window / 3f) > room);
        assertEquals(room, height);
    }

    private static int dp(int value) {
        return Math.round(value * DENSITY);
    }
}
