package com.campusbill.app.chat.ai.assistant;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 最小日记钩子：只读应用私有目录里的 assistant/diary.txt。
 * 没有编辑页。正文只有在用户确认 request_diary_context 之后才会进入对话。
 */
public final class DiaryNoteStore {

    private DiaryNoteStore() {
    }

    public static String read(Context context) {
        File file = new File(context.getApplicationContext().getFilesDir(), "assistant/diary.txt");
        if (!file.isFile()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(line);
            }
        } catch (Exception e) {
            return "";
        }
        return text.toString().trim();
    }
}
