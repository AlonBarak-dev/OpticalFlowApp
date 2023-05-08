package com.example.opticalflow;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public class FraneBack implements OpticalFlow{

    private Mat prevFrame, currFrame, flow_gray, prevGray, currGray, flow_rgb, motion_vector;
    private Mat[] output;
    private final double pyr_scale = 0.5;
    private final int levels = 3;
    private final int winSize = 15;
    private final int iterations = 3;
    private final int poly_n = 5;
    private final double poly_sigma = 1.2;
    private final int flags = 0;

    public FraneBack(){
        prevFrame = new Mat();
        currFrame = new Mat();
        flow_gray = new Mat();
        flow_rgb = new Mat();
        prevGray = new Mat();
        currGray = new Mat();
        motion_vector = new Mat(400, 400, CvType.CV_8UC1);
        output = new Mat[2];
    }

    public Mat[] run(Mat new_frame){
        Log.d("RUN-OF", "started");
        currFrame = new_frame;

        // if this is the first run
        if(prevFrame.empty()){
            currFrame.copyTo(prevFrame);
        }

        // convert images to gray
        Imgproc.cvtColor(currFrame, currGray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.cvtColor(prevFrame, prevGray, Imgproc.COLOR_RGBA2GRAY);

        // calculate optical flow from prevFrame to currFrame
        Video.calcOpticalFlowFarneback(prevGray, currGray, flow_gray, pyr_scale, levels, winSize, iterations, poly_n, poly_sigma, flags);

        // draw the optical flow
        currFrame.copyTo(flow_rgb);
        drawOptFlowMap(flow_gray, flow_rgb, 16, new Scalar(0, 255, 0));

        // update the variables for the next loop
        currFrame.copyTo(prevFrame);

        // create the output array
        output[0] = flow_rgb;
        output[1] = motion_vector;
        return output;

    }

    @Override
    public void reset_motion_vector() {
        // TBD
    }

    @Override
    public void UpdateFeatures() {
        // Do nothing
    }


    private void drawOptFlowMap(Mat flow, Mat flowmap, int step, Scalar color) {
        for (int y = 0; y < flowmap.rows(); y += step) {
            for (int x = 0; x < flowmap.cols(); x += step) {
                double[] f = flow.get(y, x);
                double fx = f[0];
                double fy = f[1];
                Point start = new Point(x, y);
                Point end = new Point(Math.round(x + fx), Math.round(y + fy));
                Imgproc.line(flowmap, start, end, color);
                Imgproc.circle(flowmap, start, 2, color, -1);
            }
        }
    }





}
