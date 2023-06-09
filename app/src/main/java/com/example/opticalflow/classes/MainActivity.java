package com.example.opticalflow.classes;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.opticalflow.classes.velocity_estimator.Basic_fusion;
import com.example.opticalflow.classes.velocity_estimator.FraneBack;
import com.example.opticalflow.classes.velocity_estimator.IMU_estimator;
import com.example.opticalflow.classes.velocity_estimator.KLT;
import com.example.opticalflow.classes.velocity_estimator.MotionVectorViz;
import com.example.opticalflow.dataTypes.velocity_estimator.OF_output;
import com.example.opticalflow.interfaces.velocity_estimator.OpticalFlow;
import com.example.opticalflow.R;
import com.example.opticalflow.interfaces.velocity_estimator.Sensor_fusion;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

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
    private Switch of_type;
    private SeekBar sensitivity_bar;
    private TextView vel_pred_text;
    private Mat curr_frame, mv_mat;
    private OF_output output;
    private OpticalFlow optical_flow;
    private IMU_estimator imu_estimator;
    private Sensor_fusion fusion;
    private float[] fuse_output;
    private MotionVectorViz mv_viewer;


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
        init_vars();
    }

    private void init_vars(){
        // first initialize with KLT optical flow
        optical_flow = new KLT(vel_pred_text);
        output = new OF_output();

        // init fusion algorithm
        fusion = new Basic_fusion();
        fuse_output = new float[3];

        // init motion vector viewer
        mv_viewer = new MotionVectorViz(400, 400);
        mv_mat = Mat.zeros(400, 400, CvType.CV_8UC1);
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

        // init switch
        of_type = (Switch) findViewById(R.id.of_type);
        of_type.setOnClickListener(this);

        // init seek bar
        sensitivity_bar = (SeekBar) findViewById(R.id.sensitivity_bar);
        sensitivity_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("SEEK", String.valueOf(progress));
                optical_flow.set_sensitivity(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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
        // get IMU variables
        float[] velocity = imu_estimator.getVelocity();
        float[] imu_position = imu_estimator.getPosition();
        // Convert the velocity to mph
        float xVelocityMph = velocity[0] * 2.23694f;
        float yVelocityMph = velocity[1] * 2.23694f;
        float zVelocityMph = velocity[2] * 2.23694f;

        Log.d("POS", String.valueOf(imu_position[0]) + ", " + String.valueOf(imu_position[1]) + ", " + String.valueOf(imu_position[2]));

        // Get the magnitude of the velocity vector
        float speedMph = (float) Math.sqrt(xVelocityMph * xVelocityMph + yVelocityMph * yVelocityMph + zVelocityMph * zVelocityMph);
        vel_pred_text.setText(String.valueOf(speedMph));

        // get OF output
        curr_frame = inputFrame.rgba();
        output = optical_flow.run(curr_frame);

        if (output.of_frame != null) {

            // fuse the IMU sensor with the Optical Flow
            fuse_output = fusion.getPosition(velocity, imu_position, output.position);

            // get Motion Vector Mat to present
            mv_mat = mv_viewer.getMotionVector(output.position);

            // draw Motion Vector
            Bitmap dst = Bitmap.createBitmap(mv_mat.width(), mv_mat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mv_mat, dst);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    motionVector.setImageBitmap(dst);
                }
            });

            return output.of_frame;
        }
        return inputFrame.rgba();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.resetMV:
                optical_flow.reset_motion_vector();
                mv_viewer.reset_motion_vector();
                break;
            case R.id.update_features_button:
                optical_flow.UpdateFeatures();
                break;
            case R.id.of_type:
                if (of_type.isChecked()){
                    optical_flow = new FraneBack();
                }
                else{
                    optical_flow = new KLT(vel_pred_text);
                }
                break;
            default:
                break;
        }
    }
}
