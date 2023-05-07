package com.example.opticalflow;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {
    private final String TAG = MainActivity.class.getSimpleName();

    static {
        // load OpenCV
        if (OpenCVLoader.initDebug()){
            Log.d("OpenCV", "success");
        }
        else{
            Log.d("OpenCV", "failed");
        }
    }
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView motionVector;
    private Button reset_button, update_features_button;
    private TextView vel_pred_text;
    private Mat curr_frame;
    private Mat[] output_klt;
    private KLT optical_flow;
    private IMU_estimator imu_estimator;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");

                    mOpenCvCameraView.enableView();

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);
        init_ui();
        // KLT
        optical_flow = new KLT(vel_pred_text);
        output_klt = new Mat[2];
    }

    private void init_ui(){
        // velocity prediction label
        vel_pred_text = (TextView)findViewById(R.id.vel_pred);
        // reset Button
        reset_button = (Button)findViewById(R.id.resetMV);
        reset_button.setOnClickListener(this);
        // update features Button
        update_features_button = (Button)findViewById(R.id.update_features_button);
        update_features_button.setOnClickListener(this);
        // Image view
        motionVector = (ImageView) findViewById(R.id.motion_vector);
        motionVector.setVisibility(View.VISIBLE);
        // Java view
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.CAMERA);    // permission
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // init IMU_estimator
        imu_estimator = new IMU_estimator(this.getApplicationContext());
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug())
        {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this,
                            mLoaderCallback);
        }
        else
        {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    @Override
    public void onCameraViewStarted(int width, int height)
    {
        Log.d(TAG, "onCameraViewStarted");
    }

    @Override
    public void onCameraViewStopped()
    {
        Log.d(TAG, "onCameraViewStopped");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        float[] velocity = imu_estimator.getVelocity();
        // Convert the velocity to mph
        float xVelocityMph = velocity[0] * 2.23694f;
        float yVelocityMph = velocity[1] * 2.23694f;
        float zVelocityMph = velocity[2] * 2.23694f;

        // Get the magnitude of the velocity vector
        float speedMph = (float) Math.sqrt(xVelocityMph * xVelocityMph + yVelocityMph * yVelocityMph + zVelocityMph * zVelocityMph);
        vel_pred_text.setText(String.valueOf(speedMph));


        curr_frame = inputFrame.rgba();
        output_klt = optical_flow.run(curr_frame);
        if (output_klt[0] != null) {
            if (output_klt[1] != null) {
                Bitmap dst = Bitmap.createBitmap(output_klt[1].width(), output_klt[1].height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(output_klt[1], dst);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        motionVector.setImageBitmap(dst);
                    }
                });
            }
            return output_klt[0];
        }
        return inputFrame.rgba();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.resetMV:
                optical_flow.reset_motion_vector();
                break;
            case R.id.update_features_button:
                optical_flow.UpdateFeatures();
                break;
            default:
                break;
        }
    }
}
