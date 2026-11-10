package com.campusbill.app.chat.voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 已安装的 RecognitionService 按荣耀 / 华为、讯飞、百度、Google、其余排序。
 * 没有可用服务时不要再试空的默认识别器，直接走离线。
 */
public final class SystemRecognizerPlan {

    public static final class Service {
        public final String packageName;
        public final String className;

        public Service(String packageName, String className) {
            this.packageName = packageName;
            this.className = className;
        }
    }

    private SystemRecognizerPlan() {
    }

    public static boolean offlineImmediately(List<Service> installed) {
        return order(installed).isEmpty();
    }

    public static List<Service> order(List<Service> installed) {
        List<Service> usable = new ArrayList<>();
        if (installed != null) {
            for (Service service : installed) {
                if (service == null || service.packageName == null || service.className == null) {
                    continue;
                }
                if (service.packageName.trim().isEmpty() || service.className.trim().isEmpty()) {
                    continue;
                }
                usable.add(service);
            }
        }
        usable.sort((left, right) -> Integer.compare(score(right.packageName), score(left.packageName)));
        return usable;
    }

    public static int score(String packageName) {
        String pkg = packageName == null ? "" : packageName.toLowerCase(Locale.US);
        if (pkg.contains("huawei") || pkg.contains("hihonor") || pkg.contains("honor")) {
            return 50;
        }
        if (pkg.contains("iflytek") || pkg.contains("xfyun")) {
            return 40;
        }
        if (pkg.contains("baidu")) {
            return 30;
        }
        if (pkg.contains("google")) {
            return 20;
        }
        return 1;
    }
}
