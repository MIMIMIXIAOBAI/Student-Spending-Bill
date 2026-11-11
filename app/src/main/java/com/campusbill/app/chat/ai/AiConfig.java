package com.campusbill.app.chat.ai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * AI 配置。DeepSeek API Key 经 {@link DeepSeekKeyStore} 放进 Android Keystore，
 * 本类的偏好里只留接口地址和模型名，不留明文密钥。
 */
public final class AiConfig {

    private static final String PREF_NAME = "ai_config";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_MODEL = "model";

    /** 默认接口地址：OpenAI 兼容的 Chat Completions 地址，可在弹窗内修改 */
    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com/chat/completions";
    /** 默认模型名称，可在弹窗内修改 */
    public static final String DEFAULT_MODEL = "deepseek-chat";

    private AiConfig() {
    }

    /** 读取 Keystore 里的 API Key，未设置时返回空字符串 */
    public static String getApiKey(Context context) {
        return DeepSeekKeyStore.get(context);
    }

    /** 保存 API Key 到 Keystore，传入空字符串表示清空 */
    public static void saveApiKey(Context context, String apiKey) {
        DeepSeekKeyStore.save(context, apiKey);
    }

    /** 读取接口地址，未设置时返回默认值 */
    public static String getBaseUrl(Context context) {
        String url = prefs(context).getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        return url == null || url.trim().isEmpty() ? DEFAULT_BASE_URL : url.trim();
    }

    /** 读取模型名称，未设置时返回默认值 */
    public static String getModel(Context context) {
        String model = prefs(context).getString(KEY_MODEL, DEFAULT_MODEL);
        return model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim();
    }

    /** 保存 Keystore 中的密钥，以及接口地址和模型名。密钥不会写进偏好明文。 */
    public static void saveSettings(Context context, String apiKey, String baseUrl, String model) {
        DeepSeekKeyStore.save(context, apiKey);
        prefs(context).edit()
                .remove(KEY_API_KEY)
                .putString(KEY_BASE_URL, baseUrl == null ? "" : baseUrl.trim())
                .putString(KEY_MODEL, model == null ? "" : model.trim())
                .apply();
    }

    /** 本机是否已配置 API KEY */
    public static boolean hasApiKey(Context context) {
        return !getApiKey(context).isEmpty();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
