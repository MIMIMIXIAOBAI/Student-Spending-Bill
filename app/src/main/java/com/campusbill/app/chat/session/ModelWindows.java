package com.campusbill.app.chat.session;

import java.util.Locale;

/** 当前 DeepSeek 模型的上下文窗口。deepseek-chat / reasoner 为 128K。 */
public final class ModelWindows {

    public static final int DEEPSEEK_128K = 131072;

    private ModelWindows() {
    }

    public static int tokensFor(String model) {
        if (model == null || model.trim().isEmpty()) {
            return DEEPSEEK_128K;
        }
        String name = model.toLowerCase(Locale.US);
        if (name.contains("deepseek")) {
            return DEEPSEEK_128K;
        }
        return DEEPSEEK_128K;
    }
}
