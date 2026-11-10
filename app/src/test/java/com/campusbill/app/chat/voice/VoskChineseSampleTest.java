package com.campusbill.app.chat.voice;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 没有系统 RecognitionService 时的离线证明：用打包策略里的同一份中文小模型
 * 把短 WAV 转成非空文本。没放模型或原生库时跳过，避免 Android 单测机去加载手机 so。
 *
 * 本地强制跑：
 * java -Dvosk.model=/path/to/vosk-model-small-cn-0.22 \
 *   -cp junit:vosk-0.3.45.jar:jna.jar:classes \
 *   org.junit.runner.JUnitCore com.campusbill.app.chat.voice.VoskChineseSampleTest
 */
public class VoskChineseSampleTest {

    @Test
    public void 无系统识别器时Vosk能转出短中文() throws Exception {
        assertTrue(SystemRecognizerPlan.offlineImmediately(CollectionsEmpty()));
        File model = modelDir();
        Assume.assumeTrue("设置 -Dvosk.model 指向解压后的 vosk-model-small-cn-0.22",
                model != null && model.isDirectory());
        Assume.assumeTrue("桌面 libvosk 不可用", nativeLibraryLoads());
        Path wav = Files.createTempFile("canteen", ".wav");
        try (InputStream input = VoskChineseSampleTest.class.getResourceAsStream("/voice/canteen.wav")) {
            Assume.assumeTrue(input != null);
            Files.copy(input, wav, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        String text = VoskWaveTranscriber.transcribe(model, wav.toFile());
        assertFalse(text.isEmpty());
        assertTrue(text.contains("食堂"));
    }

    private static java.util.List<SystemRecognizerPlan.Service> CollectionsEmpty() {
        return java.util.Collections.emptyList();
    }

    private static boolean nativeLibraryLoads() {
        try {
            Class.forName("org.vosk.LibVosk");
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    private static File modelDir() {
        String configured = System.getProperty("vosk.model");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("VOSK_MODEL");
        }
        if (configured == null || configured.trim().isEmpty()) {
            return null;
        }
        return new File(configured.trim());
    }
}
