package com.example.tirestickerrecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Console;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

    private static final String TAG = "Dependencies";
    private static final int requestPermissionID = 101;
    public static final int SWIPE_THRESHOLD = 100;
    public static final int VELOCITY_THRESHOLD = 100;

    CameraSource mCameraSource;
    SurfaceView mCameraView;
    //TextView mTextView;
    TextView frontTireTV;
    TextView rearTireTV;
    TextView frontTV;
    TextView rearTV;
    Button scanBtn;
    Button stopBtn;
    Button resetBtn;
    Button websiteBtn;

    LinearLayout resultLayout;
    Toolbar toolbar;

    GestureDetector gestureDetector;

    private DatabaseReference mDatabase;
    private DatabaseReference mDatabaseRead;

    long tireId;
    String frontSizeOk;
    String rearSizeOk;

    String frontWidth, frontRatio, frontRim;
    String rearWidth, rearRatio, rearRim;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing all the layouts

        mCameraView = findViewById(R.id.surfaceView);
        //mTextView = findViewById(R.id.text_view);
        frontTireTV = findViewById(R.id.frontTireTV);
        rearTireTV = findViewById(R.id.rearTireTV);
        rearTV = findViewById(R.id.rearTV);
        frontTV = findViewById(R.id.frontTV);


        scanBtn = findViewById(R.id.scanBtn);
        stopBtn = findViewById(R.id.stopBtn);
        resetBtn = findViewById(R.id.resetBtn);
        websiteBtn = findViewById(R.id.accessBtn);


        resultLayout = findViewById(R.id.resultLayout);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Tire information");
        toolbar.setLogo(R.drawable.ic_tire);
        toolbar.setSubtitle("Scan your tire sticker");

        setSupportActionBar(toolbar);

        gestureDetector = new GestureDetector(this);

        // Initializing Firebase binding
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //Reading database to get current ID:
        mDatabaseRead = FirebaseDatabase.getInstance().getReference().child("currentId");

        mDatabaseRead.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                tireId = (long) dataSnapshot.getValue();
                Log.w("TIREID", ""+tireId);
                mDatabaseRead.setValue(tireId+1);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        // Starting Camera
        startCameraSource();

        // Setting buttons properties
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultLayout.setVisibility(View.VISIBLE);
                //mTextView.setVisibility(View.VISIBLE);
                scanBtn.setEnabled(false);
                scanBtn.setTextColor(getResources().getColor(R.color.colorLihtOrange));

            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraSource.stop();
                scanBtn.setVisibility(View.GONE);
                resetBtn.setVisibility(View.VISIBLE);
                websiteBtn.setVisibility(View.VISIBLE);
                stopBtn.setVisibility(View.GONE);
                writeTire(tireId, frontSizeOk ,rearSizeOk);
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 Intent newIntent = new Intent(getApplicationContext(), MainActivity.class);
                 startActivity(newIntent);
             }
         });

        websiteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((frontRim != null) && (frontRatio != null) && (frontWidth != null) && (rearRim != null) && (rearRatio != null) && (rearWidth != null)) {
                    String url = "https://simpletire.com/catalog?width=" + frontWidth + "&ratio=" + frontRatio + "&rim=" + frontRim + "&rwidth=" + rearWidth + "&rratio=" + rearRatio + "&rrim=" + rearRim + "&zip=10003&maincategory=AUTOMOBILE";
                    Intent openWebsite = new Intent(Intent.ACTION_VIEW);
                    openWebsite.setData(Uri.parse(url));
                    v.getContext().startActivity(openWebsite);
                } else {
                    Toast.makeText(v.getContext(), "Incomplete information, please try again", Toast.LENGTH_LONG).show();
                }
            }
        });




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
                        frontTireTV.post(new Runnable() {
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

                                //mTextView.setText(stringBuilder.toString());
                                if(!frontSize.equals("")) {
                                    frontSizeOk = frontSize;
                                    frontTireTV.setText(frontSize);
                                }
                                if(!rearSize.equals("")){
                                    rearSizeOk = rearSize;
                                    rearTireTV.setText(rearSize);
                                }

                                if(!frontSize.equals("") && !rearSize.equals("")){
                                    rearTireTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_name , 0);
                                    frontTireTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_name, 0);

//                                    frontTireTV.setTextColor(getResources().getColor(R.color.colorGreen));
//                                    rearTireTV.setTextColor(getResources().getColor(R.color.colorGreen));
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
            if(recognizedString.charAt(index) == 'P'){
                return recognizedString.substring(index, index + 13);
            } else if (recognizedString.charAt(index + 3) == '/') {
                return recognizedString.substring(index, index + 12);
            }
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
            if(recognizedString.charAt(index) == 'P'){
                return recognizedString.substring(index, index + 13);
            } else if (recognizedString.charAt(index + 3) == '/') {
                return recognizedString.substring(index, index + 12);
            }
        }
        return "";

    }

    private void writeTire(long id, String frontSize, String rearSize) {
        String tireId = "tire "+id;
        Tire tire = new Tire(id, frontSize, rearSize);
        extractInformation(tire);
        mDatabase.child("tires").push();
        mDatabase.child("tires").child(tireId).setValue(tire);
    }



    // Following methods are implemented for the swipe gesture
    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent downEvent, MotionEvent moveEvent, float velocityX, float velocityY) {
        boolean result = false;
        float diffY = moveEvent.getY() - downEvent.getY(); //to know movement on Y axis
        float diffX = moveEvent.getX() - downEvent.getX(); //to know movement on X axis

        if(Math.abs(diffX) > Math.abs(diffY)){
            if (Math.abs(diffX)> SWIPE_THRESHOLD && Math.abs(velocityX) > VELOCITY_THRESHOLD){
                if(diffX > 0){

                }else{
                    onSwipeLeft();
                    result = true;
                }
            }
            //to know if we have a right or left swipe
        } else {
            if (Math.abs(diffY)> SWIPE_THRESHOLD && Math.abs(velocityY) > VELOCITY_THRESHOLD){
                if(diffY > 0){
                    onSwipeBottom();
                    result = true;
                } else {
                    onSwipeTop();
                    result = true;
                }
            }
        }

        return result;
    }

    private void onSwipeTop() {
        if(!scanBtn.isEnabled()) {
            resultLayout.setVisibility(View.VISIBLE);
        }
        //mTextView.setVisibility(View.VISIBLE);
    }

    private void onSwipeBottom() {
        resultLayout.setVisibility(View.GONE);
        //mTextView.setVisibility(View.GONE);
    }

    private void onSwipeLeft() {
        Intent activityListIntent = new Intent(getApplicationContext(), ListScan.class);
        startActivity(activityListIntent);
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.historyItem:
                Intent newIntent = new Intent(getApplicationContext(), ListScan.class);
                startActivity(newIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    public void extractInformation(Tire tire){
        String frontString = tire.getFrontSize();
        String rearString = tire.getRearSize();
        String frontWidth, frontRatio, frontRim;
        String rearWidth, rearRatio, rearRim;
        int frontRatioIndex, frontRimIndex;
        int rearRatioIndex, rearRimIndex;

        if(frontString!=null) {
            // Getting the Width
            if (frontString.charAt(0) == 'P') {
                frontWidth = frontString.substring(1, 4);
            } else {
                frontWidth = frontString.substring(0, 3);
            }

            // Getting the Aspect Ratio
            frontRatioIndex = frontString.indexOf("/");
            frontRatio = frontString.substring(frontRatioIndex+1, frontRatioIndex + 3);

            // Getting the Rim
            frontRimIndex = frontString.indexOf("R");
            frontRim = frontString.substring(frontRimIndex+1, frontRimIndex + 3);

            this.frontRatio = frontRatio;
            this.frontRim = frontRim;
            this.frontWidth = frontWidth;

            Log.w("TIRESINFO", "Width: " + frontWidth + "Ratio: " + frontRatio + "Rim: " + frontRim);
        }
        if (rearString!=null) {
            // Getting the Width
            if (rearString.charAt(0) == 'P') {
                rearWidth = rearString.substring(1, 4);
            } else {
                rearWidth = rearString.substring(0, 3);
            }

            // Getting the Aspect Ratio
            rearRatioIndex = rearString.indexOf("/");
            rearRatio = rearString.substring(rearRatioIndex + 1, rearRatioIndex + 3);

            // Getting the Rim
            rearRimIndex = rearString.indexOf("R");
            rearRim = rearString.substring(rearRimIndex + 1, rearRimIndex + 3);

            this.rearRatio = rearRatio;
            this.rearRim = rearRim;
            this.rearWidth = rearWidth;

            Log.w("TIRESINFOREAR", "Width: " + rearWidth + "Ratio: " + rearRatio + "Rim: " + rearRim);
        }


    }
}
