package com.campusbill.app.ui;

/**
 * 手机姿态相对「当前握持」的视线。阈值集中在这里，方便换成 UI 给的精确数。
 * 8° 以内是抖动，不驱动；达到 12° 才看向倾斜方向；约 30° 对应视线 ±1。
 * 回到 8° 以内则松开，由页面用 {@link #RETURN_MS} 把视线收回正前方。
 */
public final class AttitudeGazeMath {

    public static final float DEAD_ZONE_DEGREES = 8f;
    public static final float ACTIVATE_DEGREES = 12f;
    public static final float FULL_GAZE_DEGREES = 30f;
    /** 输出低通，避免眼睛跟着传感器噪声抖。 */
    public static final float OUTPUT_LOW_PASS = 0.45f;
    public static final float CALM_DEGREES_PER_SECOND = 10f;
    public static final long CALM_HOLD_MS = 200L;
    public static final long RETURN_MS = 400L;

    public static final class State {
        public boolean hasReference;
        public float refX;
        public float refY;
        public float refZ;
        public boolean driving;
        public float smoothNx;
        public float smoothNy;
        public long calmSinceMs = -1L;
        public long lastTimeMs;
        public float lastAngle;
        public boolean hasLast;
    }

    public static final class Step {
        public final boolean driving;
        public final boolean released;
        public final float nx;
        public final float ny;
        public final float angleDegrees;

        public Step(boolean driving, boolean released, float nx, float ny, float angleDegrees) {
            this.driving = driving;
            this.released = released;
            this.nx = nx;
            this.ny = ny;
            this.angleDegrees = angleDegrees;
        }
    }

    private AttitudeGazeMath() {
    }

    public static void clear(State state) {
        state.hasReference = false;
        state.driving = false;
        state.smoothNx = 0f;
        state.smoothNy = 0f;
        state.calmSinceMs = -1L;
        state.hasLast = false;
        state.lastAngle = 0f;
    }

    /**
     * @param upX 设备坐标里的「向上」方向，与静止时重力传感器同号
     * @param allowed 页面可见且表情是待机 02
     */
    public static Step step(State state, float upX, float upY, float upZ, long timeMs, boolean allowed) {
        float len = (float) Math.sqrt(upX * upX + upY * upY + upZ * upZ);
        if (len < 1.0e-4f) {
            return new Step(state.driving, false, state.smoothNx, state.smoothNy, state.lastAngle);
        }
        upX /= len;
        upY /= len;
        upZ /= len;
        if (!allowed) {
            boolean released = state.driving;
            clear(state);
            return new Step(false, released, 0f, 0f, 0f);
        }
        if (!state.hasReference) {
            state.refX = upX;
            state.refY = upY;
            state.refZ = upZ;
            state.hasReference = true;
            state.lastTimeMs = timeMs;
            state.lastAngle = 0f;
            state.hasLast = true;
            state.calmSinceMs = timeMs;
            return new Step(false, false, 0f, 0f, 0f);
        }
        float dot = state.refX * upX + state.refY * upY + state.refZ * upZ;
        if (dot > 1f) {
            dot = 1f;
        } else if (dot < -1f) {
            dot = -1f;
        }
        float angle = (float) Math.toDegrees(Math.acos(dot));
        float speed = 0f;
        if (state.hasLast && timeMs > state.lastTimeMs) {
            speed = Math.abs(angle - state.lastAngle) / ((timeMs - state.lastTimeMs) / 1000f);
        }
        state.lastTimeMs = timeMs;
        state.lastAngle = angle;
        state.hasLast = true;

        // 0.05° 只抵消浮点误差，产品阈值仍是 12°。
        if (!state.driving && angle + 0.05f >= ACTIVATE_DEGREES) {
            state.driving = true;
            state.calmSinceMs = -1L;
        } else if (state.driving && angle < DEAD_ZONE_DEGREES) {
            state.driving = false;
            state.refX = upX;
            state.refY = upY;
            state.refZ = upZ;
            state.smoothNx = 0f;
            state.smoothNy = 0f;
            state.calmSinceMs = timeMs;
            state.lastAngle = 0f;
            return new Step(false, true, 0f, 0f, angle);
        }

        if (!state.driving && angle < DEAD_ZONE_DEGREES && speed <= CALM_DEGREES_PER_SECOND) {
            if (state.calmSinceMs < 0L) {
                state.calmSinceMs = timeMs;
            } else if (timeMs - state.calmSinceMs >= CALM_HOLD_MS) {
                state.refX = upX;
                state.refY = upY;
                state.refZ = upZ;
                state.lastAngle = 0f;
                state.calmSinceMs = timeMs;
                angle = 0f;
            }
        } else if (!state.driving) {
            state.calmSinceMs = -1L;
        }

        if (!state.driving) {
            return new Step(false, false, 0f, 0f, angle);
        }

        float screenX = -(upX - state.refX);
        float screenY = -(upY - state.refY);
        float screenLen = (float) Math.sqrt(screenX * screenX + screenY * screenY);
        float mag = Math.min(1f, angle / FULL_GAZE_DEGREES);
        float targetX = 0f;
        float targetY = 0f;
        if (screenLen > 1.0e-4f) {
            targetX = mag * screenX / screenLen;
            targetY = mag * screenY / screenLen;
        }
        state.smoothNx += OUTPUT_LOW_PASS * (targetX - state.smoothNx);
        state.smoothNy += OUTPUT_LOW_PASS * (targetY - state.smoothNy);
        return new Step(true, false, state.smoothNx, state.smoothNy, angle);
    }
}
