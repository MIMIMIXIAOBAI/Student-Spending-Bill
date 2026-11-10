package com.campusbill.app.chat.ai.assistant;

/** 把 DeepSeek 的 HTTP 结果收成密钥 / 配额 / 网络三类。 */
public final class FailureClassifier {

    private FailureClassifier() {
    }

    public static FailureKind classifyHttp(int code, String body) {
        if (code == 401 || code == 403) {
            return FailureKind.API_KEY;
        }
        String text = body == null ? "" : body.toLowerCase();
        if (code == 402 || text.contains("insufficient") || text.contains("quota")
                || text.contains("余额") || text.contains("欠费") || text.contains("quota")) {
            return FailureKind.QUOTA;
        }
        return FailureKind.NETWORK;
    }

    public static String message(FailureKind kind) {
        if (kind == FailureKind.API_KEY) {
            return "密钥无效或未授权，请在 DeepSeek 密钥向导里重新测通。";
        }
        if (kind == FailureKind.QUOTA) {
            return "DeepSeek 余额或配额不足，请到服务商控制台检查。";
        }
        return "网络异常，这次没有连上 DeepSeek。";
    }

    /** 出错这一档在状态行左侧的短原因。不要把「重试」写进这里。 */
    public static String statusLine(FailureKind kind) {
        if (kind == FailureKind.API_KEY) {
            return "密钥失败";
        }
        if (kind == FailureKind.QUOTA) {
            return "余额不足";
        }
        return "网络错误";
    }
}
