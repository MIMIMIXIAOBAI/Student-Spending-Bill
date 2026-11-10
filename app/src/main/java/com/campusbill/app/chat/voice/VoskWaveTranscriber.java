package com.campusbill.app.chat.voice;

import org.vosk.Model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** 用本地模型把一段 WAV 转成文本。给离线回归测试和手动验证用。 */
public final class VoskWaveTranscriber {

    private VoskWaveTranscriber() {
    }

    public static String transcribe(File modelDir, File wav) throws IOException {
        try (InputStream input = new FileInputStream(wav)) {
            return transcribe(modelDir, input);
        }
    }

    public static String transcribe(File modelDir, InputStream wav) throws IOException {
        WavPcm audio = WavPcm.read(wav);
        if (audio.sampleRate != VoskModels.SAMPLE_RATE || audio.channels != 1 || audio.bitsPerSample != 16) {
            throw new IOException("需要 16 kHz 16-bit 单声道 WAV");
        }
        try (Model model = new Model(modelDir.getAbsolutePath());
             VoskPcmSession session = new VoskPcmSession(model)) {
            int offset = 0;
            while (offset < audio.pcm.length) {
                int length = Math.min(4096, audio.pcm.length - offset);
                byte[] slice = new byte[length];
                System.arraycopy(audio.pcm, offset, slice, 0, length);
                session.accept(slice, length);
                offset += length;
            }
            return session.finish();
        }
    }
}
