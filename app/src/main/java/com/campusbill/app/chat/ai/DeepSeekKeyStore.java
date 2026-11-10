package com.campusbill.app.chat.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * DeepSeek 密钥只放在 Android Keystore 的 AES 密钥后面。
 * SharedPreferences 里存的是密文，不是明文。
 */
public final class DeepSeekKeyStore {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "campusbill.deepseek.aes";
    private static final String PREF = "deepseek_keystore";
    private static final String CIPHERTEXT = "ciphertext";
    private static final String IV = "iv";
    private static final String LEGACY_PREF = "ai_config";
    private static final String LEGACY_KEY = "api_key";
    private static final int GCM_TAG_BITS = 128;

    private DeepSeekKeyStore() {
    }

    public static boolean hasKey(Context context) {
        return !get(context).isEmpty();
    }

    public static String get(Context context) {
        SharedPreferences prefs = prefs(context);
        String ciphertext = prefs.getString(CIPHERTEXT, "");
        String iv = prefs.getString(IV, "");
        if (ciphertext != null && !ciphertext.isEmpty() && iv != null && !iv.isEmpty()) {
            try {
                return decrypt(ciphertext, iv);
            } catch (Exception e) {
                return "";
            }
        }
        String legacy = legacyPrefs(context).getString(LEGACY_KEY, "");
        if (legacy != null && !legacy.trim().isEmpty()) {
            String migrated = legacy.trim();
            save(context, migrated);
            return migrated;
        }
        return "";
    }

    public static void save(Context context, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            clear(context);
            return;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey());
            byte[] encrypted = cipher.doFinal(apiKey.trim().getBytes(StandardCharsets.UTF_8));
            prefs(context).edit()
                    .putString(CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .apply();
            legacyPrefs(context).edit().remove(LEGACY_KEY).apply();
        } catch (Exception e) {
            throw new IllegalStateException("Keystore 写入失败");
        }
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(CIPHERTEXT).remove(IV).apply();
        legacyPrefs(context).edit().remove(LEGACY_KEY).apply();
    }

    private static String decrypt(String ciphertext, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey(),
                new GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)));
        byte[] plain = cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP));
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static SecretKey secretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        if (!keyStore.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE);
            generator.init(new android.security.keystore.KeyGenParameterSpec.Builder(
                    ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                            | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            generator.generateKey();
        }
        return (SecretKey) keyStore.getKey(ALIAS, null);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static SharedPreferences legacyPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(LEGACY_PREF, Context.MODE_PRIVATE);
    }
}
