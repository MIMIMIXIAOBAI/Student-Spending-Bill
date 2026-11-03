package com.campusbill.app.ui;

import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public final class PressScaleHelper {
    private static final float PRESSED = 0.96f;
    private static final long DURATION_MS = 100L;

    private PressScaleHelper() {}

    public static void attach(View view) {
        if (view == null) {
            return;
        }
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    animate(v, PRESSED);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animate(v, 1f);
                    break;
                default:
                    break;
            }
            return false; // allow click listeners to fire
        });
    }

    private static void animate(View v, float scale) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, View.SCALE_X, scale);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, View.SCALE_Y, scale);
        sx.setDuration(DURATION_MS);
        sy.setDuration(DURATION_MS);
        sx.setInterpolator(new DecelerateInterpolator());
        sy.setInterpolator(new DecelerateInterpolator());
        sx.start();
        sy.start();
    }
}
