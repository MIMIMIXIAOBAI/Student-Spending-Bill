package com.campusbill.app.chat.voice;

/** 随包的 Vosk 中文小模型。构建时下载进 assets，不把压缩包提交进 git。 */
public final class VoskModels {

    public static final String MODEL_NAME = "vosk-model-small-cn-0.22";
    public static final String ASSET_ZIP = "vosk/" + MODEL_NAME + ".zip";
    public static final String SOURCE_URL =
            "https://alphacephei.com/vosk/models/" + MODEL_NAME + ".zip";
    public static final String PROJECT_URL = "https://github.com/alphacep/vosk-api";
    public static final int SAMPLE_RATE = 16000;

    private VoskModels() {
    }
}
