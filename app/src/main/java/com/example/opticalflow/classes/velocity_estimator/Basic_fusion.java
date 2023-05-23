package com.example.opticalflow.classes.velocity_estimator;

import com.example.opticalflow.interfaces.velocity_estimator.Sensor_fusion;

import org.opencv.core.Point;

public class Basic_fusion implements Sensor_fusion {


    public Basic_fusion(){};

    @Override
    public float[] getPosition(float[] imu_velocity, float[] imu_position, Point of_position) {





        return new float[0];
    }
}
