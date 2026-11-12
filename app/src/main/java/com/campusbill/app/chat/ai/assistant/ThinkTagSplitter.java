package com.campusbill.app.chat.ai.assistant;

/** 把流式正文里的 &lt;think&gt; 与可见回答拆开。标记可以被切到两个分片里。 */
public final class ThinkTagSplitter {

    private final StringBuilder hold = new StringBuilder();
    private final StringBuilder think = new StringBuilder();
    private final StringBuilder visible = new StringBuilder();
    private boolean inThink;

    public void push(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        hold.append(chunk);
        drain();
    }

    public String think() {
        return think.toString();
    }

    public String visible() {
        return visible.toString();
    }

    private void drain() {
        while (hold.length() > 0) {
            String token = inThink ? "</think>" : "<think>";
            String text = hold.toString();
            int at = text.indexOf(token);
            if (at >= 0) {
                append(text.substring(0, at));
                hold.delete(0, at + token.length());
                inThink = !inThink;
                continue;
            }
            int keep = suffixPrefix(text, token);
            int emit = text.length() - keep;
            if (emit <= 0) {
                return;
            }
            append(text.substring(0, emit));
            hold.delete(0, emit);
            return;
        }
    }

    private void append(String text) {
        if (text.isEmpty()) {
            return;
        }
        if (inThink) {
            think.append(text);
        } else {
            visible.append(text);
        }
    }

    private static int suffixPrefix(String text, String token) {
        int max = Math.min(text.length(), token.length() - 1);
        for (int len = max; len > 0; len--) {
            if (text.endsWith(token.substring(0, len))) {
                return len;
            }
        }
        return 0;
    }
}
