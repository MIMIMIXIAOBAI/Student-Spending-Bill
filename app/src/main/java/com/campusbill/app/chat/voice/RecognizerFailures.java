package com.campusbill.app.chat.voice;

/**
 * 与 android.speech.SpeechRecognizer 的错误码一致。
 * 这些错误要换下一个系统引擎；换完仍失败再进离线识别。
 */
public final class RecognizerFailures {

    public static final int NETWORK_TIMEOUT = 1;
    public static final int NETWORK = 2;
    public static final int AUDIO = 3;
    public static final int SERVER = 4;
    public static final int CLIENT = 5;
    public static final int SPEECH_TIMEOUT = 6;
    public static final int NO_MATCH = 7;
    public static final int BUSY = 8;
    public static final int PERMISSION = 9;
    public static final int SERVER_DISCONNECTED = 11;
    public static final int LANGUAGE_NOT_SUPPORTED = 12;
    public static final int LANGUAGE_UNAVAILABLE = 13;

    private RecognizerFailures() {
    }

    public static boolean switchEngine(int error) {
        switch (error) {
            case NETWORK_TIMEOUT:
            case NETWORK:
            case AUDIO:
            case SERVER:
            case CLIENT:
            case SPEECH_TIMEOUT:
            case BUSY:
            case SERVER_DISCONNECTED:
            case LANGUAGE_NOT_SUPPORTED:
            case LANGUAGE_UNAVAILABLE:
                return true;
            default:
                return false;
        }
    }
}
