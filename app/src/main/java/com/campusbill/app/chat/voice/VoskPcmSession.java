package com.campusbill.app.chat.voice;

import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;

/** 把 16 kHz 16-bit 单声道 PCM 送给已经加载好的 Vosk 模型。 */
public final class VoskPcmSession implements AutoCloseable {

    private final Recognizer recognizer;

    public VoskPcmSession(Model model) throws IOException {
        recognizer = new Recognizer(model, VoskModels.SAMPLE_RATE);
    }

    public void accept(byte[] pcm, int length) {
        if (pcm == null || length <= 0) {
            return;
        }
        recognizer.acceptWaveForm(pcm, length);
    }

    public String partial() {
        return VoskResultText.text(recognizer.getPartialResult());
    }

    public String finish() {
        return VoskResultText.text(recognizer.getFinalResult());
    }

    @Override
    public void close() {
        recognizer.close();
    }
}
