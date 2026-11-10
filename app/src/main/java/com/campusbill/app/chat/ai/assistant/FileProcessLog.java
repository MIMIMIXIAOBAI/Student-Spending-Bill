package com.campusbill.app.chat.ai.assistant;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** 把 JSONL 写到应用私有目录，对应仓库里被 gitignore 的 process/runtime。 */
public final class FileProcessLog implements Consumer<String> {

    private final File file;

    public FileProcessLog(Context context) {
        File dir = directory(context);
        String name = OffsetDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")) + ".jsonl";
        file = new File(dir, name);
    }

    public static File directory(Context context) {
        File dir = new File(context.getApplicationContext().getFilesDir(),
                "assistant/process/runtime");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }

    public File file() {
        return file;
    }

    @Override
    public void accept(String line) {
        if (line == null) {
            return;
        }
        String safe = SecretRedactor.redact(line);
        synchronized (this) {
            try (FileOutputStream output = new FileOutputStream(file, true)) {
                output.write(safe.getBytes(StandardCharsets.UTF_8));
                output.write('\n');
            } catch (Exception ignored) {
                // 留痕失败不打断对话
            }
        }
    }
}
