package com.example.tirestickerrecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.Console;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Dependencies";
    CameraSource mCameraSource;
    SurfaceView mCameraView;
    TextView mTextView;
    TextView frontTireTV;
    TextView rearTireTV;


    private static final int requestPermissionID = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.text_view);
        frontTireTV = findViewById(R.id.frontTireTV);
        rearTireTV = findViewById(R.id.rearTireTV);


        startCameraSource();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != requestPermissionID) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mCameraSource.start(mCameraView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void startCameraSource(){

        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Dependencies not yet loaded");

        } else {
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }


                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0 ){
                        mTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                String recognition;
                                String frontSize;
                                String rearSize;

                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i=0;i<items.size();i++){
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                recognition = stringBuilder.toString();

                                frontSize = frontTireOk(recognition);
                                rearSize = rearTireOk(recognition);

                                mTextView.setText(stringBuilder.toString());
                                if(!frontSize.equals("")) {
                                    frontSize = " Front: " + frontSize;
                                    frontTireTV.setText(frontSize);
                                }
                                if(!rearSize.equals("")){
                                    rearSize = " Rear: "+rearSize;
                                    rearTireTV.setText(rearSize);
                                }

                            }
                        });
                    }
                }
            });

        }
    }

    public String frontTireOk(String recognizedString){
        if(recognizedString.contains("FRONT")) {
            int index = recognizedString.indexOf("FRONT ") + 6;
            for (int i = index; i < index + 9; i++) {
                if (recognizedString.charAt(i) == ' ') {
                    return "";
                }
            }

            if (recognizedString.charAt(index + 3) == '/')
                return recognizedString.substring(index, index + 12);
        }
        return "";

    }

    public String rearTireOk(String recognizedString){
        if(recognizedString.contains("REAR")) {
            int index = recognizedString.indexOf("REAR ") + 5;
            for (int i = index; i < index + 9; i++) {
                if (recognizedString.charAt(i) == ' ') {
                    return "";
                }
            }

            if (recognizedString.charAt(index + 3) == '/')
                return recognizedString.substring(index, index + 12);
        }
        return "";

    }
}
