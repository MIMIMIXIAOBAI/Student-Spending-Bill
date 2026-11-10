package com.campusbill.app.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * 用手机姿态驱动情绪球视线。优先旋转矢量，否则游戏旋转矢量，再否则重力加陀螺仪。
 * 手指按在球上时不写视线。离开页面就停传感器。
 */
public final class BallAttitudeGaze implements SensorEventListener {

    public interface Host {
        void onAttitudeGaze(float nx, float ny);

        void onAttitudeReturn();

        boolean fingerOwnsBall();

        /** 球可见，且当前表情是待机 02。 */
        boolean attitudeAllowed();
    }

    private final Host host;
    private final AttitudeGazeMath.State state = new AttitudeGazeMath.State();
    private SensorManager sensors;
    private boolean pageVisible;
    private boolean fused;
    private float upX;
    private float upY;
    private float upZ;
    private boolean haveUp;
    private float gyroX;
    private float gyroY;
    private float gyroZ;
    private boolean haveGyro;
    private long lastFusionNs;

    public BallAttitudeGaze(Host host) {
        this.host = host;
    }

    public void setPageVisible(Context context, boolean visible) {
        if (visible == pageVisible) {
            return;
        }
        pageVisible = visible;
        AttitudeGazeMath.clear(state);
        haveUp = false;
        haveGyro = false;
        if (!visible) {
            stopSensors();
            return;
        }
        startSensors(context);
    }

    public boolean isDriving() {
        return state.driving;
    }

    /** 表情或可见性变了，马上按新条件收视线或重新记握持姿势。 */
    public void onConditionsChanged() {
        if (!pageVisible) {
            return;
        }
        if (host.attitudeAllowed()) {
            return;
        }
        if (state.driving || state.hasReference) {
            AttitudeGazeMath.clear(state);
            if (!host.fingerOwnsBall()) {
                host.onAttitudeReturn();
            }
        }
    }

    public void stop() {
        pageVisible = false;
        AttitudeGazeMath.clear(state);
        stopSensors();
    }

    private void startSensors(Context context) {
        if (context == null) {
            return;
        }
        sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensors == null) {
            return;
        }
        Sensor rotation = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotation != null) {
            fused = false;
            sensors.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME);
            return;
        }
        Sensor game = sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (game != null) {
            fused = false;
            sensors.registerListener(this, game, SensorManager.SENSOR_DELAY_GAME);
            return;
        }
        fused = true;
        Sensor gravity = sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gravity == null) {
            gravity = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        Sensor gyro = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gravity != null) {
            sensors.registerListener(this, gravity, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyro != null) {
            sensors.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void stopSensors() {
        if (sensors != null) {
            sensors.unregisterListener(this);
            sensors = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!pageVisible || event == null || event.sensor == null) {
            return;
        }
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ROTATION_VECTOR || type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            float[] matrix = new float[9];
            try {
                SensorManager.getRotationMatrixFromVector(matrix, event.values);
            } catch (RuntimeException ignored) {
                return;
            }
            publish(matrix[2], matrix[5], matrix[8], event.timestamp / 1_000_000L);
            return;
        }
        if (type == Sensor.TYPE_GYROSCOPE) {
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];
            haveGyro = true;
            return;
        }
        if (!fused || (type != Sensor.TYPE_GRAVITY && type != Sensor.TYPE_ACCELEROMETER)) {
            return;
        }
        float measuredX = event.values[0];
        float measuredY = event.values[1];
        float measuredZ = event.values[2];
        if (haveUp && haveGyro && lastFusionNs > 0L) {
            float dt = (event.timestamp - lastFusionNs) / 1_000_000_000f;
            if (dt > 0f && dt < 0.2f) {
                float ox = gyroY * upZ - gyroZ * upY;
                float oy = gyroZ * upX - gyroX * upZ;
                float oz = gyroX * upY - gyroY * upX;
                float predictedX = upX - ox * dt;
                float predictedY = upY - oy * dt;
                float predictedZ = upZ - oz * dt;
                measuredX = predictedX * 0.85f + measuredX * 0.15f;
                measuredY = predictedY * 0.85f + measuredY * 0.15f;
                measuredZ = predictedZ * 0.85f + measuredZ * 0.15f;
            }
        }
        upX = measuredX;
        upY = measuredY;
        upZ = measuredZ;
        haveUp = true;
        lastFusionNs = event.timestamp;
        publish(upX, upY, upZ, event.timestamp / 1_000_000L);
    }

    private void publish(float x, float y, float z, long timeMs) {
        AttitudeGazeMath.Step step = AttitudeGazeMath.step(
                state, x, y, z, timeMs, host.attitudeAllowed());
        if (host.fingerOwnsBall()) {
            return;
        }
        if (step.released) {
            host.onAttitudeReturn();
        } else if (step.driving) {
            host.onAttitudeGaze(step.nx, step.ny);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
