package com.campusbill.app.chat.ai.assistant;

import java.time.LocalDate;

/** daily_fortune 的本地文案。塔罗只是 style=tarot_lite。 */
public final class FortuneComposer {

    private static final String[] FORTUNE = {
            "步子放稳，小额支出先记下来就好。",
            "适合把今天的午餐写清楚，心里会轻一点。",
            "宜整理零钱，不宜再加一笔冲动消费。"
    };
    private static final String[] MOOD = {
            "晴。节奏松一点，吃饭别赶。",
            "多云。有点累，但还能把账记完。",
            "微风。适合慢慢吃午饭。"
    };
    private static final String[] TAROT = {
            "星币", "圣杯", "宝剑", "权杖"
    };

    private FortuneComposer() {
    }

    public static String compose(LocalDate date, String style) {
        int seed = Math.floorMod((date == null ? 0 : date.hashCode()) * 31
                + (style == null ? 0 : style.hashCode()), 12);
        if ("tarot_lite".equals(style)) {
            return "今日运势 · 轻量塔罗：" + TAROT[seed % TAROT.length] + "。这只是运势里的一种玩法。";
        }
        if ("mood".equals(style)) {
            return "情绪晴雨表：" + MOOD[seed % MOOD.length];
        }
        return "今日运势：" + FORTUNE[seed % FORTUNE.length];
    }
}
