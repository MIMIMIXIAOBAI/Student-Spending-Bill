package com.campusbill.app.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatBallFadeTest {

    @Test
    public void 渐隐从球体下沿拉到上沿_高面板和160dp都跟着走() {
        int width = 1080;
        int tall = 900;
        int compressed = 480;
        ChatBallFade.Shade tallShade = ChatBallFade.shade(0, width, tall);
        ChatBallFade.Shade shortShade = ChatBallFade.shade(0, width, compressed);

        assertEquals(Math.round(ChatBallFade.sphereTopPx(width, tall)), tallShade.fadeTopPx);
        assertEquals(Math.round(ChatBallFade.sphereBottomPx(width, tall)), tallShade.fadeBottomPx());
        assertEquals(Math.round(ChatBallFade.sphereTopPx(width, compressed)), shortShade.fadeTopPx);
        assertEquals(Math.round(ChatBallFade.sphereBottomPx(width, compressed)), shortShade.fadeBottomPx());

        assertTrue(tallShade.fadeHeightPx > 0);
        assertTrue(shortShade.fadeHeightPx > 0);
        assertTrue(tallShade.fadeHeightPx > shortShade.fadeHeightPx);
        assertTrue(tallShade.fadeHeightPx != 48 && tallShade.fadeHeightPx != 144);
        assertTrue(shortShade.fadeHeightPx != 48 && shortShade.fadeHeightPx != 144);
        assertEquals(tallShade.fadeHeightPx / (float) tall,
                shortShade.fadeHeightPx / (float) compressed, 0.02f);
    }

    @Test
    public void 球体上沿到舞台顶是实心浅灰没有透明缝() {
        ChatBallFade.Shade shade = ChatBallFade.shade(0, 1080, 900);
        assertTrue(shade.capHeightPx > 0);
        assertEquals(0, shade.capTopPx);
        assertEquals(shade.fadeTopPx, shade.capTopPx + shade.capHeightPx);
        assertTrue(shade.fadeTopPx < shade.fadeBottomPx());
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
