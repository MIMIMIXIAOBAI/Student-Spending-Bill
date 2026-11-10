package com.campusbill.app.chat.session;

/** 只估算一段即将发送的文本本身，不用消息条数。 */
public final class TokenEstimate {

    private TokenEstimate() {
    }

    public static int ofText(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int tokens = 0;
        int latin = 0;
        for (int i = 0; i < text.length(); ) {
            int point = text.codePointAt(i);
            i += Character.charCount(point);
            if (point > 0x2E80) {
                tokens += 1;
            } else {
                latin++;
            }
        }
        tokens += (latin + 3) / 4;
        return tokens;
    }
}
