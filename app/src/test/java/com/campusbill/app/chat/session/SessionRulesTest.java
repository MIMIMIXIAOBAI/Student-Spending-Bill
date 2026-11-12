package com.campusbill.app.chat.session;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionRulesTest {

    @Test
    public void 当前会话写盘后再读回同一条消息和滚动位置() {
        ChatSessionBook book = new ChatSessionBook();
        ChatSessionBook.Session session = book.restore(0);
        book.append(ChatSessionBook.TYPE_AI, "你好");
        book.append(ChatSessionBook.TYPE_USER, "这周食堂花了多少");
        book.setScroll(4);
        long id = book.currentId();

        ChatSessionBook restored = ChatSessionBook.fromArchive(book.archive());
        ChatSessionBook.Session again = restored.restore(id);
        assertEquals(id, restored.currentId());
        assertEquals(2, again.messages.size());
        assertEquals("这周食堂花了多少", again.messages.get(1).content);
        assertEquals(4, again.scrollPosition);
        assertEquals(id, again.id);
    }

    @Test
    public void 标题用首条用户消息_空标题退回这句话() {
        ChatSessionBook book = new ChatSessionBook();
        book.restore(0);
        book.append(ChatSessionBook.TYPE_AI, "欢迎");
        book.append(ChatSessionBook.TYPE_USER, "记一笔午餐");
        assertEquals("记一笔午餐", book.current().title);
        book.rename(book.currentId(), "午饭");
        assertEquals("午饭", book.current().title);
        book.rename(book.currentId(), "  ");
        assertEquals("记一笔午餐", book.current().title);
    }

    @Test
    public void 百分之八十只预存不切换_百分之九十五才带记忆开新场() {
        ChatSessionBook book = new ChatSessionBook();
        book.restore(0);
        book.append(ChatSessionBook.TYPE_USER, "我常在食堂吃饭");
        long original = book.currentId();
        assertEquals(MemoryHandoff.Step.PRESTORE, MemoryHandoff.afterReply(80, false));
        assertEquals(MemoryHandoff.Step.NONE, MemoryHandoff.afterReply(79, false));
        book.remember("习惯：食堂", original);
        assertEquals(original, book.currentId());
        assertEquals(1, book.current().messages.size());

        assertEquals(MemoryHandoff.Step.AUTO_SWITCH, MemoryHandoff.afterReply(95, true));
        ChatSessionBook.Session next = book.open(true);
        assertFalse(next.id == original);
        assertTrue(next.messages.isEmpty());
        assertEquals("习惯：食堂", next.injectedMemory);
        assertTrue(next.showMemoryTip);
        assertEquals(1, book.find(original).messages.size());

        assertEquals(MemoryHandoff.Step.EXTRACT_THEN_SWITCH, MemoryHandoff.afterReply(96, false));
        assertEquals(MemoryHandoff.Step.AUTO_SWITCH,
                MemoryHandoff.beforeSend(9600, 100, 10000, true));
        assertEquals(MemoryHandoff.Step.NONE,
                MemoryHandoff.beforeSend(1000, 10, 10000, true));
    }

    @Test
    public void 百分比按token和窗口_不用消息条数() {
        assertEquals(62, ContextUsage.percent(6200, 10000));
        assertEquals(0, ContextUsage.percent(0, 10000));
        assertEquals(ContextUsage.Level.NORMAL, ContextUsage.level(79));
        assertEquals(ContextUsage.Level.HINT, ContextUsage.level(80));
        assertEquals(ContextUsage.Level.FULL, ContextUsage.level(95));
        assertTrue(ContextUsage.wouldNotFit(9000, 600, 10000));
        assertFalse(ContextUsage.wouldNotFit(1000, 20, 10000));
        assertEquals(2, TokenEstimate.ofText("你好"));
        assertEquals(131072, ModelWindows.tokensFor("deepseek-chat"));
        int messages = 40;
        assertTrue(ContextUsage.percent(6200, 10000) != messages);
    }

    @Test
    public void 默认对话路径不再挂语音引擎() throws Exception {
        String activity = read("app/src/main/java/com/campusbill/app/AiChatActivity.java");
        String layout = read("app/src/main/res/layout/activity_ai_chat.xml");
        String gradle = read("app/build.gradle");
        String manifest = read("app/src/main/AndroidManifest.xml");
        String joined = activity + layout + gradle + manifest;
        assertFalse(joined.contains("SpeechRecognizer"));
        assertFalse(joined.contains("Vosk"));
        assertFalse(joined.contains("btnHoldTalk"));
        assertFalse(joined.contains("RECORD_AUDIO"));
        assertFalse(joined.contains("fetchVoskChineseModel"));
        assertTrue(layout.contains("btnSend"));
        assertTrue(layout.contains("etMessage"));
    }

    private static String read(String relative) throws Exception {
        Path direct = Path.of(relative);
        if (Files.exists(direct)) {
            return Files.readString(direct);
        }
        return Files.readString(Path.of("..", relative));
    }
}
