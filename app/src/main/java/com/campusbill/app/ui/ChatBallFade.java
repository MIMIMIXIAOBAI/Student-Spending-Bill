package com.campusbill.app.ui;

/**
 * 对话页球体在方形 WebView 里的位置。渐隐从球体下沿拉到球体上沿，不写死 dp。
 * 数字来自 emotion-ball 的 viewBox（-15,-15,259,259）和 blob 轮廓。
 */
public final class ChatBallFade {

    static final float VIEW_ORIGIN = -15f;
    static final float VIEW_SIZE = 259f;
    /** blob 轮廓中心，与 rings.js 的 HEAD_C 一致。 */
    static final float SPHERE_CENTER = 114.2705f;
    /** blob 轮廓最高点。 */
    static final float SPHERE_TOP = 0.01f;
    /** blob 轮廓最低点。 */
    static final float SPHERE_BOTTOM = 228.49f;

    /** 顶栏到球体上沿的实心浅灰，以及球体上沿到下沿的线性渐隐。 */
    public static final class Shade {
        public final int capTopPx;
        public final int capHeightPx;
        public final int fadeTopPx;
        public final int fadeHeightPx;

        Shade(int capTopPx, int capHeightPx, int fadeTopPx, int fadeHeightPx) {
            this.capTopPx = capTopPx;
            this.capHeightPx = capHeightPx;
            this.fadeTopPx = fadeTopPx;
            this.fadeHeightPx = fadeHeightPx;
        }

        public int fadeBottomPx() {
            return fadeTopPx + fadeHeightPx;
        }
    }

    private ChatBallFade() {
    }

    /** 球体上沿相对面板顶边的像素。面板更高或更矮时，上沿跟着变。 */
    public static float sphereTopPx(int panelWidth, int panelHeight) {
        return edgePx(panelWidth, panelHeight, SPHERE_TOP);
    }

    /** 球体下沿相对面板顶边的像素。面板更高或更矮时，下沿跟着变。 */
    public static float sphereBottomPx(int panelWidth, int panelHeight) {
        return edgePx(panelWidth, panelHeight, SPHERE_BOTTOM);
    }

    /**
     * 舞台坐标。实心盖从舞台顶（顶栏下沿）铺到球体上沿；
     * 渐隐从球体上沿拉到球体下沿，高度跟着当前面板，不写死 dp。
     */
    public static Shade shade(int panelTopPx, int panelWidth, int panelHeight) {
        int fadeTop = Math.round(panelTopPx + sphereTopPx(panelWidth, panelHeight));
        int fadeBottom = Math.round(panelTopPx + sphereBottomPx(panelWidth, panelHeight));
        int fadeHeight = Math.max(0, fadeBottom - fadeTop);
        int capHeight = Math.max(0, fadeTop);
        return new Shade(0, capHeight, fadeTop, fadeHeight);
    }

    private static float edgePx(int panelWidth, int panelHeight, float viewY) {
        if (panelWidth <= 0 || panelHeight <= 0) {
            return 0f;
        }
        float side = Math.min(panelWidth, panelHeight);
        float originY = (panelHeight - side) / 2f;
        return originY + (viewY - VIEW_ORIGIN) / VIEW_SIZE * side;
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
