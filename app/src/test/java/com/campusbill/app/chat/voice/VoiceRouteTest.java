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
        assertTrue(RecognizerFailures.switchEngine(RecognizerFailures.SPEECH_TIMEOUT));
        assertFalse(RecognizerFailures.switchEngine(RecognizerFailures.NO_MATCH));
        assertFalse(RecognizerFailures.switchEngine(RecognizerFailures.PERMISSION));
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
