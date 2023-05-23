package com.example.opticalflow.classes.velocity_estimator;

import android.util.Log;
import android.widget.TextView;

import com.example.opticalflow.dataTypes.velocity_estimator.OF_output;
import com.example.opticalflow.interfaces.velocity_estimator.OpticalFlow;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import java.util.concurrent.Semaphore;


public class KLT implements OpticalFlow {

    private Mat prevFrame, currFrame, prevGray, currGray;
    private Mat[] output;
    private MatOfPoint2f prevPts, currPts;
    private MatOfByte status;
    private byte[] status_array;
    private Point[] prevPts_arr, currPts_arr;
    private MatOfFloat err;
    private Scalar color;
    private int flow_pts, max_corners;
    private boolean update_features, is_valid;
    private Point prevMv, currMv;
    private int limit;
    private double x_avg1, x_avg2, y_avg1, y_avg2;
    private Semaphore semaphore;
    private TextView vel_label;
    private OF_output of_output;


    public KLT(TextView vel_label_init){
        vel_label = vel_label_init;
        prevFrame = new Mat();
        currFrame = new Mat();
        prevGray = new Mat();
        currGray = new Mat();
        output = new Mat[2];
        of_output = new OF_output();
        prevPts = new MatOfPoint2f();
        currPts = new MatOfPoint2f();
        color = new Scalar(240,230,140);
        status = new MatOfByte();
        err = new MatOfFloat();
        update_features = false;
        is_valid = false;
        max_corners = 50;
        flow_pts = max_corners;
        semaphore = new Semaphore(1);
    }

    public void set_sensitivity(int value){
        try{
            semaphore.acquire();
            max_corners = value;
            semaphore.release();
        } catch (Exception e){
            Log.e("SENSITIVITY", "Failed to acquire semaphore");
        }
    }


    public void reset_motion_vector(){
        prevMv = null;
        currMv = null;
    }

    public void UpdateFeatures(){
        this.update_features = true;
    }
    public void update_points(Mat prevGray, Mat currGray, MatOfPoint2f prevPts){

        currGray.copyTo(prevGray);
        // detect features in the first frame
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(prevGray, corners, max_corners, 0.1, 5);
        prevPts.fromArray(corners.toArray());
    }

    public OF_output run(Mat new_frame){
        // init
        Log.d("RUN-OF", "started");
        currFrame = new_frame;

        // convert the frame to Gray
        Imgproc.cvtColor(currFrame, currGray, Imgproc.COLOR_RGBA2GRAY);

        // if this is the first loop, find good features
        if (prevGray.empty()){
            this.update_points(prevGray, currGray, prevPts);
            of_output.of_frame = null;
            of_output.position = null;
            return of_output;
        }
        try{
            semaphore.acquire();
            limit = max_corners / 5;
            if (flow_pts < limit || this.update_features){
                this.update_points(prevGray, currGray, prevPts);
                this.update_features = false;
            }
            semaphore.release();
        }
        catch (Exception e){
            Log.e("SENSITIVITY", "Failed to acquire semaphore");
        }

        // Run the KLT algorithm for Optical Flow
        Video.calcOpticalFlowPyrLK(prevGray, currGray, prevPts, currPts, status, err);

        // draw the flow vectors
        flow_pts = 0;
        x_avg1 = 0;
        x_avg2 = 0;
        y_avg2 = 0;
        y_avg1 = 0;
        status_array = status.toArray();
        prevPts_arr = prevPts.toArray();
        currPts_arr = currPts.toArray();
        for (int i =0; i < prevPts.rows(); i++){
            if (status_array[i] == 1){
                Point pt1 = new Point(prevPts_arr[i].x, prevPts_arr[i].y);
                Point pt2 = new Point(currPts_arr[i].x, currPts_arr[i].y);
                x_avg1 += pt1.x;
                x_avg2 += pt2.x;
                y_avg1 += pt1.y;
                y_avg2 += pt2.y;
                Imgproc.line(currFrame, pt1, pt2, color, 10);
                flow_pts++;
            }
        }

        // Calculate the motion vector
        x_avg1 /= flow_pts;
        y_avg1 /= flow_pts;
        x_avg2 /= flow_pts;
        y_avg2 /= flow_pts;

        currMv = new Point((x_avg1 - x_avg2)/10, (y_avg1 - y_avg2)/10);
        if (prevMv == null){
            currMv.x += 200;
            currMv.y += 200;
        }
        else{
            currMv.x += prevMv.x;
            currMv.y += prevMv.y;
        }
        prevMv = currMv;

        Log.d("RUN-OF", "Processed");
        // update variables for next iteration
        currGray.copyTo(prevGray);
        prevPts.fromArray(currPts.toArray());

        of_output.of_frame = currFrame;
        of_output.position = currMv;
        return of_output;

    }









}
