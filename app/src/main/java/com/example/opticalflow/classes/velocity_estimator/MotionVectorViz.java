package com.example.opticalflow.classes.velocity_estimator;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MotionVectorViz {

    private Mat motion_vector;
    private Point prevMV, currMV;
    private Scalar color;

    public MotionVectorViz(int rows, int cols){
        // initialize new Motion vector matrix
        motion_vector = Mat.zeros(rows, cols, CvType.CV_8UC1);
        // test case
        color = new Scalar(240,230,140);
        prevMV = null;
        currMV = null;
    }

    public void reset_motion_vector(){
        motion_vector = Mat.zeros(400, 400, CvType.CV_8UC1);
        prevMV = null;
        currMV = null;
    }

    public Mat getMotionVector(Point new_pos){

        currMV = new_pos;
        // first iteration
        if (prevMV == null){
            prevMV = currMV;
        }

        Imgproc.line(motion_vector, prevMV, currMV, color, 4);

        prevMV = currMV;

        return motion_vector;
    }

}
