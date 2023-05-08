package com.example.opticalflow.interfaces;

import org.opencv.core.Mat;

public interface OpticalFlow {
    public Mat[] run(Mat new_frame);
    public void reset_motion_vector();
    public void UpdateFeatures();

    public void set_sensitivity(int value);

}
