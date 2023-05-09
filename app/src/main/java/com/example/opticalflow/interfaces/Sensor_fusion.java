package com.example.opticalflow.interfaces;

public interface Sensor_fusion {


    /**
     * This method perform Sensor fusion between the IMU and the Optical Flow sensor.
     * Eventually, it outputs an estimated position base on each sensor output.
     * @param imu_velocity
     * @param imu_position
     * @param of_position
     * @return
     */
    public float[] getPosition(float[] imu_velocity, float[] imu_position, float[] of_position);



}
