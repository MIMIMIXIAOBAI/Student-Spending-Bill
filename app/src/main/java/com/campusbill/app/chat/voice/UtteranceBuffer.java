package com.campusbill.app.chat.voice;

/** 按停顿收进来的几段识别结果。松手时拼成一条，不在中途发出去。 */
public final class UtteranceBuffer {

    private final StringBuilder committed = new StringBuilder();
    private String partial = "";

    public void sealPartial() {
        commit(partial);
    }

    public void commit(String piece) {
        String clean = piece == null ? "" : piece.trim();
        partial = "";
        if (clean.isEmpty()) {
            return;
        }
        if (committed.length() > 0) {
            committed.append(' ');
        }
        committed.append(clean);
    }

    public void setPartial(String piece) {
        partial = piece == null ? "" : piece.trim();
    }

    public String text() {
        return join(committed.toString(), partial);
    }

    public boolean isEmpty() {
        return text().isEmpty();
    }

    public void clear() {
        committed.setLength(0);
        partial = "";
    }

    public static String join(String earlier, String latest) {
        String left = earlier == null ? "" : earlier.trim();
        String right = latest == null ? "" : latest.trim();
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty() || left.endsWith(right)) {
            return left;
        }
        return left + " " + right;
    }
}
