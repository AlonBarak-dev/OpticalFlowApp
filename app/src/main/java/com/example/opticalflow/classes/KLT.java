package com.example.opticalflow.classes;

import android.util.Log;
import android.widget.TextView;

import com.example.opticalflow.interfaces.OpticalFlow;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.Queue;

public class KLT implements OpticalFlow {

    private Mat prevFrame, currFrame, prevGray, currGray, MVframe;
    private Mat[] output;
    private MatOfPoint2f prevPts, currPts;
    private MatOfByte status;
    private byte[] status_array;
    private Point[] prevPts_arr, currPts_arr;
    private MatOfFloat err;
    private Scalar color;
    private int flow_pts;
    private boolean update_features, is_valid;
    private Point prevMv, currMv;
    private double x_avg1, x_avg2, y_avg1, y_avg2, velocity;
    TermCriteria criteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS,10,0.03);
    TextView vel_label;


    public KLT(TextView vel_label_init){
        vel_label = vel_label_init;
        prevFrame = new Mat();
        currFrame = new Mat();
        prevGray = new Mat();
        currGray = new Mat();
        output = new Mat[2];
        prevPts = new MatOfPoint2f();
        currPts = new MatOfPoint2f();
        status = new MatOfByte();
        err = new MatOfFloat();
        color = new Scalar(240,230,140);
        flow_pts = 500;
        MVframe = Mat.zeros(400, 400, CvType.CV_8UC1);
        update_features = false;
        is_valid = false;
    }


    public void reset_motion_vector(){
        MVframe = Mat.zeros(400, 400, CvType.CV_8UC1);
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
        Imgproc.goodFeaturesToTrack(prevGray, corners, 500, 0.1, 5);
        prevPts.fromArray(corners.toArray());
    }

    public Mat[] run(Mat new_frame){
        // init
        Log.d("RUN-OF", "started");
        currFrame = new_frame;

        // convert the frame to Gray
        Imgproc.cvtColor(currFrame, currGray, Imgproc.COLOR_RGBA2GRAY);

        // if this is the first loop, find good features
        if (prevGray.empty()){
            this.update_points(prevGray, currGray, prevPts);
            output[0] = null;
            output[1] = null;
            return output;
        }
        if (flow_pts < 300 || this.update_features){
            this.update_points(prevGray, currGray, prevPts);
            this.update_features = false;
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
            prevMv = currMv;
        }
        else{
            currMv.x += prevMv.x;
            currMv.y += prevMv.y;
            velocity = Math.sqrt((currMv.x-200)*(currMv.x-200) + (currMv.y-200)*(currMv.y-200));
            Log.d("VEL", "" + (currMv.x-200) + "  " + (currMv.y-200));
//            vel_label.setText(String.valueOf(velocity));
            Imgproc.line(MVframe, prevMv, currMv, color, 4);
            prevMv = currMv;
        }

        Log.d("RUN-OF", "Processed");
        // update variables for next iteration
        currGray.copyTo(prevGray);
        prevPts.fromArray(currPts.toArray());
        output[0] = currFrame;
        output[1] = MVframe;
        return output;

    }









}
