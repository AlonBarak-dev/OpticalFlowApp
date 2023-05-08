package com.example.opticalflow;

import org.opencv.core.Mat;

public interface OpticalFlow {
    public Mat[] run(Mat new_frame);
    public void reset_motion_vector();
    public void UpdateFeatures();
}
