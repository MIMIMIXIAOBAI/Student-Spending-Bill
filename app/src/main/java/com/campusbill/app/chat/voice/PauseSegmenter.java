package com.campusbill.app.chat.voice;

/**
 * 按能量看停顿。说完并安静约半秒就断成一段，避免小模型把整段按住吃成一句。
 * 短语内部的短间隙不断句。
 */
public final class PauseSegmenter {

    public static final int SPEECH_RMS = 400;
    public static final int MIN_SPEECH_MS = 200;
    public static final int PAUSE_MS = 500;
    public static final int MAX_SEGMENT_MS = 8000;

    private int speechMs;
    private int silenceMs;
    private int sinceFlushMs;
    private boolean heardSpeech;

    public static final class Mark {
        public final boolean flush;

        public Mark(boolean flush) {
            this.flush = flush;
        }
    }

    public Mark push(byte[] pcm, int length) {
        int samples = length / 2;
        if (samples <= 0) {
            return new Mark(false);
        }
        long sum = 0;
        for (int i = 0; i + 1 < length; i += 2) {
            int sample = (pcm[i] & 0xff) | (pcm[i + 1] << 8);
            sum += (long) sample * sample;
        }
        int rms = (int) Math.sqrt(sum / (double) samples);
        int elapsed = samples * 1000 / VoskModels.SAMPLE_RATE;
        sinceFlushMs += elapsed;
        if (rms >= SPEECH_RMS) {
            speechMs += elapsed;
            silenceMs = 0;
            if (speechMs >= MIN_SPEECH_MS) {
                heardSpeech = true;
            }
        } else if (heardSpeech) {
            silenceMs += elapsed;
        }
        boolean pause = heardSpeech && silenceMs >= PAUSE_MS;
        boolean tooLong = heardSpeech && sinceFlushMs >= MAX_SEGMENT_MS && silenceMs >= 120;
        if (pause || tooLong) {
            noteFlushed();
            return new Mark(true);
        }
        return new Mark(false);
    }

    public void noteFlushed() {
        speechMs = 0;
        silenceMs = 0;
        sinceFlushMs = 0;
        heardSpeech = false;
    }
}
