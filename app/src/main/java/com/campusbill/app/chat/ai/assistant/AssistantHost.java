package com.campusbill.app.chat.ai.assistant;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.campusbill.app.chat.ai.AiConfig;
import com.campusbill.app.chat.ai.DeepSeekStreamClient;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** 把助手控制器留在后台线程，界面回调回到主线程。 */
public final class AssistantHost {

    public interface Surface {
        void onStatus(String statusText, boolean confirmVisible, String emotionId);

        void onTranscript(String text);

        void onConfirmCard(String title, String body);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final AssistantController controller;

    public AssistantHost(Context context, Surface surface) {
        AssistantController.Docs documents;
        try {
            documents = AssistantDocuments.load(context);
        } catch (Exception e) {
            documents = new FallbackDocs();
        }
        controller = new AssistantController(
                LocalDate.now(),
                () -> AiConfig.hasApiKey(context),
                new RoomBillLedger(context),
                DiaryNoteStore.read(context),
                documents,
                new DeepSeekStreamClient(context),
                new FileProcessLog(context),
                new AssistantController.Ui() {
                    @Override
                    public void onStatus(String statusText, boolean confirmVisible, String emotionId) {
                        if (!confirmVisible && !AssistantStatus.THINKING.label.equals(statusText)
                                && !AssistantStatus.TOOL.label.equals(statusText)) {
                            busy.set(false);
                        }
                        main.post(() -> surface.onStatus(statusText, confirmVisible, emotionId));
                    }

                    @Override
                    public void onTranscript(String text) {
                        main.post(() -> surface.onTranscript(text));
                    }

                    @Override
                    public void onConfirmCard(String title, String body) {
                        main.post(() -> surface.onConfirmCard(title, body));
                    }
                });
    }

    public boolean startOrIgnore(String text, Runnable onIgnored) {
        if (!busy.compareAndSet(false, true)) {
            main.post(onIgnored);
            return false;
        }
        executor.execute(() -> {
            try {
                controller.start(text);
            } catch (RuntimeException e) {
                busy.set(false);
            }
        });
        return true;
    }

    public void retry(Runnable onIgnored) {
        if (!busy.compareAndSet(false, true)) {
            main.post(onIgnored);
            return;
        }
        executor.execute(() -> {
            try {
                controller.retry();
            } catch (RuntimeException e) {
                busy.set(false);
            }
        });
    }

    public void confirm() {
        executor.execute(controller::confirm);
    }

    public void cancel() {
        executor.execute(controller::cancel);
    }

    /** 构建时缺了 docs 快照时的空文档。不另造工具名。 */
    private static final class FallbackDocs implements AssistantController.Docs {
        @Override
        public String systemPromptBody() {
            return "";
        }

        @Override
        public String toolsArray() {
            return "[]";
        }

        @Override
        public String version() {
            return "v0.1";
        }

        @Override
        public String emotionMap() {
            return "";
        }
    }
}
