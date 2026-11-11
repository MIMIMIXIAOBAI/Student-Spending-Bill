package com.campusbill.app.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatInputChromeTest {

    @Test
    public void 空闲时输入行是56dp且不预留状态和上下文() {
        assertEquals(56, ChatInputChrome.inputRowDp(false));
        assertEquals(8, ChatInputChrome.INPUT_PAD_VERTICAL_DP);
        assertEquals(40, ChatInputChrome.FIELD_MIN_DP);
        assertEquals(16, ChatInputChrome.INPUT_PAD_HORIZONTAL_DP);
        assertFalse(ChatInputChrome.reserveStatus(""));
        assertFalse(ChatInputChrome.reserveStatus(null));
        assertFalse(ChatInputChrome.reserveContext(""));
        assertFalse(ChatInputChrome.reserveContext("   "));
    }

    @Test
    public void 四档状态占32dp出错行也算() {
        assertEquals(32, ChatInputChrome.STATUS_DP);
        assertTrue(ChatInputChrome.reserveStatus("思考"));
        assertTrue(ChatInputChrome.reserveStatus("调用工具"));
        assertTrue(ChatInputChrome.reserveStatus("等待确认"));
        assertTrue(ChatInputChrome.reserveStatus("出错"));
        assertTrue(ChatInputChrome.reserveStatus("密钥失败"));
        assertTrue(ChatInputChrome.reserveStatus("余额不足"));
        assertTrue(ChatInputChrome.reserveStatus("网络错误"));
        assertFalse(ChatInputChrome.reserveStatus("空闲"));
    }

    @Test
    public void 多行输入最多96dp上下文有字才占位() {
        assertEquals(96, ChatInputChrome.fieldDp(true));
        assertEquals(112, ChatInputChrome.inputRowDp(true));
        assertEquals(4, ChatInputChrome.CONTEXT_PAD_VERTICAL_DP);
        assertTrue(ChatInputChrome.reserveContext("上下文 12%"));
        assertEquals(8, ChatInputChrome.LIST_TOP_DP);
    }
}
