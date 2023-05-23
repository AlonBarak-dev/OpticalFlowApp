package com.example.opticalflow.classes.velocity_estimator;

import com.example.opticalflow.dataTypes.velocity_estimator.OF_output;
import com.example.opticalflow.interfaces.velocity_estimator.OpticalFlow;

import org.opencv.core.Mat;

public class BoofOF implements OpticalFlow {
    @Override
    public OF_output run(Mat new_frame) {
        return null;
    }

    @Override
    public void reset_motion_vector() {

    }

    @Override
    public void UpdateFeatures() {

    }

    @Override
    public void set_sensitivity(int value) {

    }
}
