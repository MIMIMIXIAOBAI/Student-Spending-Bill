package com.campusbill.app.ui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AttitudeGazeMathTest {

    @Test
    public void 八度以内不驱动_十二度才看向倾斜_三十度接近满偏() {
        AttitudeGazeMath.State state = new AttitudeGazeMath.State();
        AttitudeGazeMath.Step rest = AttitudeGazeMath.step(state, 0f, 1f, 0f, 0L, true);
        assertFalse(rest.driving);

        AttitudeGazeMath.Step jitter = AttitudeGazeMath.step(state, tiltX(5f), tiltY(5f), 0f, 16L, true);
        assertFalse(jitter.driving);
        assertTrue(jitter.angleDegrees < AttitudeGazeMath.DEAD_ZONE_DEGREES);

        AttitudeGazeMath.Step between = AttitudeGazeMath.step(state, tiltX(10f), tiltY(10f), 0f, 32L, true);
        assertFalse(between.driving);
        assertFalse(between.released);

        AttitudeGazeMath.clear(state);
        AttitudeGazeMath.step(state, 0f, 1f, 0f, 0L, true);
        AttitudeGazeMath.Step active = AttitudeGazeMath.step(state, tiltX(12f), tiltY(12f), 0f, 20L, true);
        assertTrue(active.driving);
        assertTrue(active.nx > 0f);

        AttitudeGazeMath.Step held = active;
        for (int i = 0; i < 16; i++) {
            held = AttitudeGazeMath.step(state, tiltX(30f), tiltY(30f), 0f, 40L + i * 16L, true);
        }
        assertTrue(held.driving);
        assertTrue(held.nx > 0.75f);
        assertTrue(held.nx > Math.abs(held.ny));
        assertTrue(held.nx <= 1f);
    }

    @Test
    public void 松开阈值有迟滞_非待机立即停止() {
        AttitudeGazeMath.State state = new AttitudeGazeMath.State();
        AttitudeGazeMath.step(state, 0f, 1f, 0f, 0L, true);
        AttitudeGazeMath.Step started = AttitudeGazeMath.step(state, tiltX(14f), tiltY(14f), 0f, 20L, true);
        assertTrue(started.driving);

        AttitudeGazeMath.Step still = AttitudeGazeMath.step(state, tiltX(10f), tiltY(10f), 0f, 40L, true);
        assertTrue(still.driving);
        assertFalse(still.released);

        AttitudeGazeMath.Step back = AttitudeGazeMath.step(state, tiltX(6f), tiltY(6f), 0f, 60L, true);
        assertFalse(back.driving);
        assertTrue(back.released);

        AttitudeGazeMath.step(state, 0f, 1f, 0f, 80L, true);
        AttitudeGazeMath.step(state, tiltX(20f), tiltY(20f), 0f, 100L, true);
        AttitudeGazeMath.Step blocked = AttitudeGazeMath.step(state, tiltX(20f), tiltY(20f), 0f, 120L, false);
        assertFalse(blocked.driving);
        assertTrue(blocked.released);
        AttitudeGazeMath.Step idle = AttitudeGazeMath.step(state, tiltX(20f), tiltY(20f), 0f, 140L, false);
        assertFalse(idle.driving);
        assertFalse(idle.released);
    }

    private static float tiltX(float degrees) {
        return -(float) Math.sin(Math.toRadians(degrees));
    }

    private static float tiltY(float degrees) {
        return (float) Math.cos(Math.toRadians(degrees));
    }
}
