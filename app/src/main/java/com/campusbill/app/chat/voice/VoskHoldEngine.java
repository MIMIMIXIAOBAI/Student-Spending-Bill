package com.campusbill.app.chat.voice;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import org.vosk.Model;

/**
 * 按住期间用 AudioRecord 采集，松开后把 Vosk 最终文本交回。
 */
public final class VoskHoldEngine {

    public interface Listener {
        void onPartial(String text);

        void onStopped(String text);

        void onFailed();
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean stopRequested;
    private volatile boolean running;
    private Thread thread;

    public boolean isRunning() {
        return running;
    }

    public void start(Model model, Listener listener) {
        stopRequested = false;
        running = true;
        thread = new Thread(() -> capture(model, listener), "vosk-hold");
        thread.start();
    }

    public void stop() {
        stopRequested = true;
    }

    private void capture(Model model, Listener listener) {
        AudioRecord audio = null;
        VoskPcmSession session = null;
        try {
            session = new VoskPcmSession(model);
            int min = AudioRecord.getMinBufferSize(
                    VoskModels.SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int size = Math.max(min, 3200);
            audio = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    VoskModels.SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    size);
            if (audio.getState() != AudioRecord.STATE_INITIALIZED) {
                postFailed(listener);
                return;
            }
            audio.startRecording();
            byte[] buf = new byte[Math.min(size, 6400)];
            while (!stopRequested) {
                int n = audio.read(buf, 0, buf.length);
                if (n == 0) {
                    continue;
                }
                if (n < 0) {
                    postFailed(listener);
                    return;
                }
                session.accept(buf, n);
                String partial = session.partial();
                if (!partial.isEmpty()) {
                    main.post(() -> listener.onPartial(partial));
                }
            }
            String text = session.finish();
            main.post(() -> {
                running = false;
                listener.onStopped(text);
            });
        } catch (Exception e) {
            postFailed(listener);
        } finally {
            if (audio != null) {
                try {
                    audio.stop();
                } catch (RuntimeException ignored) {
                    // already stopped
                }
                audio.release();
            }
            if (session != null) {
                session.close();
            }
            running = false;
        }
    }

    private void postFailed(Listener listener) {
        running = false;
        main.post(listener::onFailed);
    }
}
