package com.campusbill.app.chat.voice;

import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VoiceRouteTest {

    @Test
    public void 没有系统识别器时直接走离线() {
        assertTrue(SystemRecognizerPlan.offlineImmediately(null));
        assertTrue(SystemRecognizerPlan.offlineImmediately(Collections.emptyList()));
        assertTrue(SystemRecognizerPlan.offlineImmediately(Collections.singletonList(
                new SystemRecognizerPlan.Service(" ", ""))));
    }

    @Test
    public void 识别服务按荣耀讯飞百度谷歌排序且不插入空默认引擎() {
        List<SystemRecognizerPlan.Service> ordered = SystemRecognizerPlan.order(Arrays.asList(
                new SystemRecognizerPlan.Service("com.google.android.googlequicksearchbox", "Google"),
                new SystemRecognizerPlan.Service("com.baidu.speech", "Baidu"),
                new SystemRecognizerPlan.Service("com.other.asr", "Other"),
                new SystemRecognizerPlan.Service("com.iflytek.speechcloud", "IFlytek"),
                new SystemRecognizerPlan.Service("com.hihonor.speech", "Honor")));
        assertEquals(5, ordered.size());
        assertEquals("com.hihonor.speech", ordered.get(0).packageName);
        assertEquals("com.iflytek.speechcloud", ordered.get(1).packageName);
        assertEquals("com.baidu.speech", ordered.get(2).packageName);
        assertEquals("com.google.android.googlequicksearchbox", ordered.get(3).packageName);
        assertEquals("com.other.asr", ordered.get(4).packageName);
        assertFalse(SystemRecognizerPlan.offlineImmediately(ordered));
    }

    @Test
    public void 网络音频和超时也要换引擎_没匹配到不算引擎故障() {
        assertTrue(RecognizerFailures.switchEngine(RecognizerFailures.CLIENT));
        assertTrue(RecognizerFailures.switchEngine(RecognizerFailures.SERVER));
        assertTrue(RecognizerFailures.switchEngine(RecognizerFailures.BUSY));
        assertTrue(RecognizerFailures.switchEngine(RecognizerFailures.LANGUAGE_UNAVAILABLE));
        assertTrue(RecognizerFailures.switchEngine(RecognizerFailures.NETWORK));
        assertTrue(RecognizerFailures.switchEngine(RecognizerFailures.NETWORK_TIMEOUT));
        assertTrue(RecognizerFailures.switchEngine(RecognizerFailures.AUDIO));
        assertTrue(RecognizerFailures.restartWhileHeld(RecognizerFailures.SPEECH_TIMEOUT));
        assertTrue(RecognizerFailures.restartWhileHeld(RecognizerFailures.NO_MATCH));
        assertFalse(RecognizerFailures.switchEngine(RecognizerFailures.SPEECH_TIMEOUT));
        assertFalse(RecognizerFailures.switchEngine(RecognizerFailures.NO_MATCH));
        assertFalse(RecognizerFailures.switchEngine(RecognizerFailures.PERMISSION));
    }

    @Test
    public void 停顿才断句_短间隙和多段文字仍拼成一条() {
        PauseSegmenter segmenter = new PauseSegmenter();
        assertFalse(segmenter.push(tone(300, 4000), tone(300, 4000).length).flush);
        assertFalse(segmenter.push(tone(200, 0), tone(200, 0).length).flush);
        assertTrue(segmenter.push(tone(600, 0), tone(600, 0).length).flush);

        UtteranceBuffer buffer = new UtteranceBuffer();
        buffer.commit("你好");
        buffer.setPartial("食堂");
        assertEquals("你好 食堂", buffer.text());
        buffer.sealPartial();
        buffer.commit("花了多少");
        assertEquals("你好 食堂 花了多少", buffer.text());
        assertEquals("你好 食堂", UtteranceBuffer.join("你好", "食堂"));
    }

    private static byte[] tone(int ms, int amplitude) {
        int samples = VoskModels.SAMPLE_RATE * ms / 1000;
        byte[] pcm = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            short sample = (short) ((i % 20 < 10) ? amplitude : -amplitude);
            pcm[i * 2] = (byte) sample;
            pcm[i * 2 + 1] = (byte) (sample >> 8);
        }
        return pcm;
    }

    @Test
    public void 解析Vosk文本并读出样例wav() throws Exception {
        assertEquals("你好", VoskResultText.text("{\"text\":\"你好\"}"));
        assertEquals("食堂", VoskResultText.text("{\"partial\":\"食堂\"}"));
        assertEquals("", VoskResultText.text(""));
        try (InputStream input = VoiceRouteTest.class.getResourceAsStream("/voice/canteen.wav")) {
            assertTrue(input != null);
            WavPcm wav = WavPcm.read(input);
            assertEquals(16000, wav.sampleRate);
            assertEquals(1, wav.channels);
            assertEquals(16, wav.bitsPerSample);
            assertTrue(wav.pcm.length > 1000);
        }
    }
}
