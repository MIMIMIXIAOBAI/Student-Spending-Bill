package com.campusbill.app.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatBallFadeTest {

    @Test
    public void 渐隐贴着球体下沿而不是方框底边() {
        int width = 1080;
        int tall = 900;
        int compressed = 480;
        float tallBottom = ChatBallFade.sphereBottomPx(width, tall);
        float shortBottom = ChatBallFade.sphereBottomPx(width, compressed);
        assertTrue(tallBottom < tall);
        assertTrue(shortBottom < compressed);
        assertTrue(shortBottom < tallBottom);
        int fade = 144;
        assertTrue(ChatBallFade.fadeTopPx(tallBottom, fade) != tall - fade);
        assertTrue(ChatBallFade.fadeTopPx(shortBottom, fade) != compressed - fade);
        assertTrue(Math.abs(ChatBallFade.fadeTopPx(shortBottom, fade) - (shortBottom - fade)) <= 1f);
    }

    @Test
    public void 球两侧不在球体命中范围内() {
        int width = 1080;
        int height = 480;
        assertTrue(ChatBallFade.insideSphere(width / 2f, height / 2f, width, height));
        assertFalse(ChatBallFade.insideSphere(8f, height / 2f, width, height));
        assertFalse(ChatBallFade.insideSphere(width - 8f, height / 2f, width, height));
    }
}
