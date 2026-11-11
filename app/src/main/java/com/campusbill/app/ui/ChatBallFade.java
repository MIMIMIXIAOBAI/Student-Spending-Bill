package com.campusbill.app.ui;

/**
 * 对话页球体在方形 WebView 里的位置。渐隐贴着球体下沿，不贴方框底边。
 * 数字来自 emotion-ball 的 viewBox（-15,-15,259,259）和 blob 轮廓。
 */
public final class ChatBallFade {

    public static final int FADE_DP = 48;

    static final float VIEW_ORIGIN = -15f;
    static final float VIEW_SIZE = 259f;
    /** blob 轮廓中心，与 rings.js 的 HEAD_C 一致。 */
    static final float SPHERE_CENTER = 114.2705f;
    /** blob 轮廓最低点。 */
    static final float SPHERE_BOTTOM = 228.49f;

    private ChatBallFade() {
    }

    /** 球体下沿相对面板顶边的像素。面板更高或更矮时，下沿跟着变。 */
    public static float sphereBottomPx(int panelWidth, int panelHeight) {
        if (panelWidth <= 0 || panelHeight <= 0) {
            return 0f;
        }
        float side = Math.min(panelWidth, panelHeight);
        float originY = (panelHeight - side) / 2f;
        return originY + (SPHERE_BOTTOM - VIEW_ORIGIN) / VIEW_SIZE * side;
    }

    /** 48dp 渐隐的顶边：从球体下沿向上，而不是从方框底边向上。 */
    public static int fadeTopPx(float sphereBottomPx, int fadePx) {
        return Math.round(sphereBottomPx - fadePx);
    }

    public static boolean insideSphere(float x, float y, int panelWidth, int panelHeight) {
        if (panelWidth <= 0 || panelHeight <= 0) {
            return false;
        }
        float side = Math.min(panelWidth, panelHeight);
        float originX = (panelWidth - side) / 2f;
        float originY = (panelHeight - side) / 2f;
        float cx = originX + (SPHERE_CENTER - VIEW_ORIGIN) / VIEW_SIZE * side;
        float cy = originY + (SPHERE_CENTER - VIEW_ORIGIN) / VIEW_SIZE * side;
        float radius = (SPHERE_BOTTOM - SPHERE_CENTER) / VIEW_SIZE * side;
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= radius * radius;
    }
}
