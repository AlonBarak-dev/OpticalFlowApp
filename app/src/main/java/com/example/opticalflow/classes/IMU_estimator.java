package com.example.opticalflow.classes;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;

import java.util.concurrent.Semaphore;

public class IMU_estimator implements SensorEventListener {


    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;


    private float[] gravity = new float[3];
    private float[] magnitude = new float[3];
    private float[] linearAcceleration = new float[3];
    private float[] rotationVector = new float[3];
    private float[] angularVelocity = new float[3];
    private float[] velocity = new float[3];
    private float[] position = new float[3];
    private long lastUpdateTime;
    private Semaphore semaphore;

    public IMU_estimator(Context context){
        // Get a reference to the SensorManager
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // Get references to the accelerometer and gyroscope sensors
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Register this class as a listener for the sensors
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        // init binary Semaphore
        semaphore = new Semaphore(1);

        // Initialize the last update time
        lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Calculate the time elapsed since the last update
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;

        // Update the last update time
        lastUpdateTime = currentTime;

        // Handle the sensor data
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // Save the gravity vector
                gravity = event.values.clone();
                break;
            case Sensor.TYPE_GYROSCOPE:
                // Save the rotation vector and angular velocity
                rotationVector = event.values.clone();
                angularVelocity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnitude = event.values.clone();
                break;
        }
        // Calculate the linear acceleration by subtracting the gravity vector
        // from the accelerometer readings
        linearAcceleration[0] = event.values[0] - gravity[0];
        linearAcceleration[1] = event.values[1] - gravity[1];
        linearAcceleration[2] = event.values[2] - gravity[2];

        // Integrate the linear acceleration to estimate the velocity
        velocity[0] += linearAcceleration[0] * deltaTime;
        velocity[1] += linearAcceleration[1] * deltaTime;
        velocity[2] += linearAcceleration[2] * deltaTime;

        // Apply a low-pass filter to the velocity estimate to reduce noise
        velocity[0] = 0.8f * velocity[0] + 0.2f * angularVelocity[0];
        velocity[1] = 0.8f * velocity[1] + 0.2f * angularVelocity[1];
        velocity[2] = 0.8f * velocity[2] + 0.2f * angularVelocity[2];

        // orientation
        float[] RotationMatrix = new float[9];
        SensorManager.getRotationMatrix(RotationMatrix, null, gravity, magnitude);
        // Express the updated rotation matrix as three orientation angles.
        final float[] orientationAngles = new float[3];
        SensorManager.getOrientation(RotationMatrix, orientationAngles);
        convertToDegrees(orientationAngles);
        Log.d("ORIENTATION", orientationAngles[0] + ", " + orientationAngles[1] + ", " + orientationAngles[2]);


        // Use the velocity estimate to update the position
        try {
            semaphore.acquire();
            position[0] += velocity[0] * deltaTime;
            position[1] += velocity[1] * deltaTime;
            position[2] += velocity[2] * deltaTime;
            semaphore.release();
        }
        catch (Exception e){
            Log.e("IMU", "Failed to acquire semaphore");
        }
    }

    private void convertToDegrees(float[] vector){
        for (int i = 0; i < vector.length; i++){
            vector[i] = Math.round(Math.toDegrees(vector[i]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    public float[] getVelocity() {
        // Return the current velocity estimate
        return velocity.clone();
    }

    public float[] getPosition() {
        // Return the current position estimate
        float[] output = new float[3];
        try{
            semaphore.acquire();
            output = position.clone();
            semaphore.release();
        }catch (Exception e){
            Log.e("IMU", "Failed to acquire semaphore");
        }
        return output;
    }

    public void stop() {
        // Unregister this class as a listener for the sensors
        sensorManager.unregisterListener(this);
    }
}
