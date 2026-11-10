package com.campusbill.app.chat.voice;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.vosk.Model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 对话页打开时在后台把 assets 里的中文小模型解到应用目录并加载。
 * 第一次按住麦克风时不再等整段解压。
 */
public final class VoskModelStore {

    private static final String TAG = "VoskModel";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final List<Waiter> WAITERS = new ArrayList<>();
    private static volatile Model model;
    private static volatile boolean failed;
    private static boolean started;

    public interface Ready {
        void onReady(Model model);
    }

    private static final class Waiter {
        final Ready ready;
        final Runnable failed;

        Waiter(Ready ready, Runnable failed) {
            this.ready = ready;
            this.failed = failed;
        }
    }

    private VoskModelStore() {
    }

    public static void prepare(Context context) {
        Context app = context.getApplicationContext();
        synchronized (VoskModelStore.class) {
            if (started || model != null) {
                return;
            }
            started = true;
        }
        Thread thread = new Thread(() -> load(app), "vosk-unpack");
        thread.setDaemon(true);
        thread.start();
    }

    public static void whenReady(Ready ready, Runnable onFailed) {
        synchronized (VoskModelStore.class) {
            if (model != null) {
                Model readyModel = model;
                MAIN.post(() -> ready.onReady(readyModel));
                return;
            }
            if (failed) {
                MAIN.post(onFailed);
                return;
            }
            WAITERS.add(new Waiter(ready, onFailed));
        }
    }

    private static void load(Context context) {
        try {
            File dir = unpack(context);
            Model loaded = new Model(dir.getAbsolutePath());
            List<Waiter> pending;
            synchronized (VoskModelStore.class) {
                model = loaded;
                pending = new ArrayList<>(WAITERS);
                WAITERS.clear();
            }
            for (Waiter waiter : pending) {
                MAIN.post(() -> waiter.ready.onReady(loaded));
            }
            Log.i(TAG, "model ready " + dir.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "model unpack failed", e);
            List<Waiter> pending;
            synchronized (VoskModelStore.class) {
                failed = true;
                pending = new ArrayList<>(WAITERS);
                WAITERS.clear();
            }
            for (Waiter waiter : pending) {
                MAIN.post(waiter.failed);
            }
        }
    }

    static File unpack(Context context) throws IOException {
        File root = new File(context.getFilesDir(), "vosk");
        File modelDir = new File(root, VoskModels.MODEL_NAME);
        File marker = new File(modelDir, "conf/model.conf");
        if (marker.isFile()) {
            return modelDir;
        }
        if (modelDir.exists() && !deleteRecursively(modelDir)) {
            throw new IOException("无法清掉上次没解完的模型");
        }
        if (!root.exists() && !root.mkdirs()) {
            throw new IOException("无法创建模型目录");
        }
        AssetManager assets = context.getAssets();
        try (InputStream raw = assets.open(VoskModels.ASSET_ZIP);
             ZipInputStream zip = new ZipInputStream(raw)) {
            String rootPath = root.getCanonicalPath() + File.separator;
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                File out = new File(root, entry.getName());
                String canonical = out.getCanonicalPath();
                if (!canonical.startsWith(rootPath)) {
                    throw new IOException("压缩包路径越界");
                }
                if (entry.isDirectory()) {
                    if (!out.exists() && !out.mkdirs()) {
                        throw new IOException("无法创建 " + out.getName());
                    }
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("无法创建目录");
                }
                try (FileOutputStream output = new FileOutputStream(out)) {
                    int n;
                    while ((n = zip.read(buf)) >= 0) {
                        if (n > 0) {
                            output.write(buf, 0, n);
                        }
                    }
                }
            }
        }
        if (!marker.isFile()) {
            throw new IOException("模型里没有 conf/model.conf");
        }
        return modelDir;
    }

    private static boolean deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!deleteRecursively(child)) {
                    return false;
                }
            }
        }
        return file.delete();
    }
}
