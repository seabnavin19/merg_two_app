/*
 * ******************************************************************
 * @title FLIR THERMAL SDK
 * @file MainActivity.java
 * @Author FLIR Systems AB
 *
 * @brief  Main UI of test application
 *
 * Copyright 2019:    FLIR Systems
 * ******************************************************************/
package com.samples.flironecamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.BuildConfig;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.image.Point;
import com.flir.thermalsdk.image.Rectangle;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

//import com.samples.flironecamera.helpers.GraphicOverlay;
//import com.samples.flironecamera.helpers.RectOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Sample application for scanning a FLIR ONE or a built in emulator
 * <p>
 * See the {@link CameraHandler} for how to preform discovery of a FLIR ONE camera, connecting to it and start streaming images
 * <p>
 * The MainActivity is primarily focused to "glue" different helper classes together and updating the UI components
 * <p/>
 * Please note, this is <b>NOT</b> production quality code, error handling has been kept to a minimum to keep the code as clear and concise as possible
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //Handles Android permission for eg Network
    public PermissionHandler permissionHandler;
    private FaceDetector faceDetectors;
    private Handler handler ;
    private Runnable runnable;
    private ImageView dcimage;
    private Bitmap mymsx;


    //Handles network camera operations
    public CameraHandler cameraHandler;

    public Identity connectedIdentity = null;
    //private TextView connectionStatus;
    //private TextView discoveryStatus;
    private TextView descFlirOneStatus;

//    private ImageView msxImage;
//    private ImageView photoImage;

    private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue(100);
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();

//    GraphicOverlay graphicOverlay;
//    public Bitmap mybitmap=null;

    public   String temperatureData ="0";

    private TextView tm;
    public int goToNextAction;

    //draw line




    public void getRe(View view) {
        Toast.makeText(MainActivity.this,temperatureData,Toast.LENGTH_LONG).show();
    }

    /**
     * Show message on the screen
     */
    public interface ShowMessage {
        void show(String message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // keep screen on forever
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        goToNextAction=1;

        //if we want to keep screen on for specific duration

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//            }
//        }, 20000);

        try{
        ThermalLog.LogLevel enableLoggingInDebug;
        if (BuildConfig.DEBUG) enableLoggingInDebug = ThermalLog.LogLevel.DEBUG;
        else enableLoggingInDebug = ThermalLog.LogLevel.NONE;

        //ThermalSdkAndroid has to be initiated from a Activity with the Application Context to prevent leaking Context,
        // and before ANY using any ThermalSdkAndroid functions
        //ThermalLog will show log from the Thermal SDK in standards android log framework
        ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);
        }catch (Exception e){}

        permissionHandler = new PermissionHandler(showMessage, MainActivity.this);

        cameraHandler = new CameraHandler();




//        Bitmap myLogo = ((BitmapDrawable)getResources().getDrawable(R.drawable.logo)).getBitmap();
//        mybitmap=myLogo;
        this.startDiscovery();
        setupViews();
//        Connect();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.stopDiscovery();
        this.disconnect();
        cameraHandler.clear();

    }





    public void startDiscovery(View view) {
        startDiscovery();
    }

    public void stopDiscovery(View view) {
        stopDiscovery();
    }


    public void connectFlirOne(View view) {
        connect(cameraHandler.getFlirOne());
//        Intent i = new Intent(this,DetectorActivity.class);
//        startActivity(i);
    }

    public void Connect(){

        connect(cameraHandler.getFlirOne());


    }

    public void connectSimulatorOne(View view) {
        connect(cameraHandler.getCppEmulator());
    }

    public void connectSimulatorTwo(View view) {
        connect(cameraHandler.getFlirOneEmulator());
    }

    public void disconnect(View view) {
        disconnect();
    }

    /**
     * Handle Android permission request response for Bluetooth permissions
     */
    /**
     * Connect to a Camera
     */
    public void connect(Identity identity) {
        //We don't have to stop a discovery but it's nice to do if we have found the camera that we are looking for
        cameraHandler.stopDiscovery(discoveryStatusListener);

        if (connectedIdentity != null) {
            Log.d(TAG, "connect(), in *this* code sample we only support one camera connection at the time");
            showMessage.show("connect(), in *this* code sample we only support one camera connection at the time");
            return;
        }

        if (identity == null) {
            Log.d(TAG, "connect(), can't connect, no camera available");
            showMessage.show("connect(), can't connect, no camera available");
            return;
        }

        connectedIdentity = identity;

        updateConnectionText(identity, "CONNECTING");
        //IF your using "USB_DEVICE_ATTACHED" and "usb-device vendor-id" in the Android Manifest
        // you don't need to request permission, see documentation for more information
        if (UsbPermissionHandler.isFlirOne(identity)) {
            usbPermissionHandler.requestFlirOnePermisson(identity, this, permissionListener);
        } else {
            doConnect(identity);

        }

    }

    private int running=0;
    private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
        @Override
        public void permissionGranted(Identity identity) {
            doConnect(identity);
        }

        @Override
        public void permissionDenied(Identity identity) {
            MainActivity.this.showMessage.show("Permission was denied for identity ");
        }

        @Override
        public void error(UsbPermissionHandler.UsbPermissionListener.ErrorType errorType, final Identity identity) {
            MainActivity.this.showMessage.show("Error when asking for permission for FLIR ONE, error:"+errorType+ " identity:" +identity);
        }
    };

    private void doConnect(Identity identity) {
        new Thread(() -> {
            try {
                cameraHandler.connect(identity, connectionStatusListener);
                runOnUiThread(() -> {
                    updateConnectionText(identity, "CONNECTED");
                    cameraHandler.startStream(streamDataListener);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Could not connect: " + e);
                    updateConnectionText(identity, "DISCONNECTED");
//                    descFlirOneStatus.setText("");
                });
            }
        }).start();
    }

    /**
     * Disconnect to a camera
     */

    private void disconnect() {
        updateConnectionText(connectedIdentity, "DISCONNECTING");
        connectedIdentity = null;
        Log.d(TAG, "disconnect() called with: connectedIdentity = [" + connectedIdentity + "]");
        new Thread(() -> {
            cameraHandler.disconnect();
//            graphicOverlay.clear();
            runOnUiThread(() -> {
                    updateConnectionText(null, "DISCONNECTED");
                    connectedIdentity=null;
//                    descFlirOneStatus.setText("");
            });
        }).start();
        temperatureData="0";

    }

    /**
     * Update the UI text for connection status
     */


    private void updateConnectionText(Identity identity, String status) {
        String deviceId = identity != null ? identity.deviceId : "";
        MainActivity.this.showMessage.show(deviceId + ": " + status);
    }

    /**
     * Start camera discovery
     */
    private void startDiscovery() {
        cameraHandler.startDiscovery(cameraDiscoveryListener, discoveryStatusListener);

    }

    /**
     * Stop camera discovery
     */
    public void stopDiscovery() {
        cameraHandler.stopDiscovery(discoveryStatusListener);
    }

    /**
     * Callback for discovery status, using it to update UI
     */
    private CameraHandler.DiscoveryStatus discoveryStatusListener = new CameraHandler.DiscoveryStatus() {
        @Override
        public void started() {
           MainActivity.this.showMessage.show("Discovering");
        }

        @Override
        public void stopped() {
            MainActivity.this.showMessage.show("Stopped Discovering");
        }
    };

    /**
     * Camera connecting state thermalImageStreamListener, keeps track of if the camera is connected or not
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use {@link #runOnUiThread(Runnable)} to interact view UI components
     */
    private ConnectionStatusListener connectionStatusListener = errorCode -> {
        Log.d(TAG, "onDisconnected errorCode:" + errorCode);

        runOnUiThread(() -> updateConnectionText(connectedIdentity, "DISCONNECTED"));
    };

    private final CameraHandler.StreamDataListener streamDataListener = new CameraHandler.StreamDataListener() {

        @Override
        public void images(FrameDataHolder dataHolder) {

            runOnUiThread(() -> {
//                msxImage.setImageBitmap(dataHolder.dcBitmap);

            });
        }
//        int go=1;
        @Override
        public void images(Bitmap msxBitmap, Bitmap dcBitmap) {

            try {
                framesBuffer.put(new FrameDataHolder(msxBitmap,dcBitmap));

            } catch (InterruptedException e) {
                //if interrupted while waiting for adding a new item in the queue
                Log.e(TAG,"images(), unable to add incoming images to frames buffer, exception:"+e);
            }

            runOnUiThread(() -> {
                Log.d(TAG,"framebuffer size:"+framesBuffer.size());

                FrameDataHolder poll = framesBuffer.poll();
                assert poll != null;
//                msxImage.setImageBitmap(poll.msxatatemperatureData = stringFourDigits(cameraHandler.getLogData());
//                Toast.makeText(MainActivity.this,temperatureData,Toast.LENGTH_LONG).show();
//                mybitmap=poll.dcBitmap;



                // start camera when the temperature equal to human temperature
//                if(goToNextAction==1){
//                    temperatureData=cameraHandler.getInfo();
//                    if (Float.parseFloat(temperatureData)>=34){
//
//                            Intent i = new Intent(MainActivity.this,login.class);
//                            startActivity(i);
//                        }
//

                    Toast.makeText(MainActivity.this,temperatureData,Toast.LENGTH_LONG).show();
                temperatureData=cameraHandler.getInfo();
                tm.setText(temperatureData);

                Log.i("tempppp",temperatureData);

//                    if (goToNextAction==1){
//
//                        if (Float.parseFloat(temperatureData)>=10){
//                            Intent u = new Intent(MainActivity.this,login.class);
//                            startActivityForResult(u,1);
//                        }
//
//                    }
//
////                }
            });
        }

    };


    public void detectFace() {
        temperatureData=cameraHandler.getInfo();
//        try {
//            goToNextAction=0;
//            InputImage image = InputImage.fromBitmap(mybitmap, 0);
//            faceDetectors.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
//
//                @Override
//                public void onSuccess(List<Face> faces) {
//                    faces= new ArrayList<>();
//                    if (faces.size() >= 1) {
//                        Face face = faces.get(0);
//
//                        final RectF boundingBoxt = new RectF(face.getBoundingBox());
//                        Point point = new Point((int) (boundingBoxt.centerX()), (int) (boundingBoxt.top));
////                    Handler handler= new Handler();
//                        Rectangle rectangle = new Rectangle((int) (boundingBoxt.left + boundingBoxt.width() / 8), (int) (boundingBoxt.top + boundingBoxt.height() / 8), (int) (boundingBoxt.width()), (int) (boundingBoxt.height()));
//
////                    cameraHandler.setWidth_height(point);
//                        try{
//                        cameraHandler.setRectangle(rectangle);
//                        }catch (Exception e){}
//
//                        if (cameraHandler.getInfo() != null) {
//                            temperatureData = cameraHandler.getInfo();
//                            if (Float.parseFloat(cameraHandler.getInfo()) >= 38.1) {
//                                temperatureData = "36.0";
//                            }
////                            if (Float.parseFloat(cameraHandler.getInfo())<= 35)
////                            if (Float.parseFloat(cameraHandler.getInfo()) < 35.4) {
////                                Float me = Float.parseFloat(cameraHandler.getInfo()) + 1.5f;
////                                temperatureData = String.valueOf(me);
////                            } else if (Float.parseFloat(cameraHandler.getInfo()) <= 35.8 && Float.parseFloat(cameraHandler.getInfo()) >= 35) {
////                                Float me = Float.parseFloat(cameraHandler.getInfo()) + 0.5f;
////                                temperatureData = String.valueOf(me);
////                            }
//
//
//                        } else {
////                            cameraHandler.setRectangle(null);
////                            mybitmap=null;
//                            temperatureData = "0";
//                        }
////                    dcimage.invalidate();
////                    BitmapDrawable drawable = (BitmapDrawable) dcimage.getDrawable();
////                    Bitmap bitmap = drawable.getBitmap();
////                        Toast.makeText(MainActivity.this,String.valueOf(point.x)+""+point.y+" "+cameraHandler.getPoint()+":"+cameraHandler.getInfo(), Toast.LENGTH_LONG).show();
//
//                    } else {
//                    Point point = null;
//                    cameraHandler.setWidth_height(point);
//                    mybitmap=null;
//
////                    dcimage.setImageBitmap(mybitmap);
////                        cameraHandler.setRectangle(null);
//                        temperatureData = "0";
//
//                    }
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                com.flir.thermalsdk.image.Point point = null;
//                cameraHandler.setWidth_height(point);
//                temperatureData="0";
//                mybitmap=null;
////                    cameraHandler.setRectangle(null);
//
//                }
//            });
//        }catch (Exception e){
//
//            temperatureData="0";
//        }
//        return temperatureData;

    }


    /**
     * Camera Discovery thermalImageStreamListener, is notified if a new camera was found during a active discovery phase
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use {@link #runOnUiThread(Runnable)} to interact view UI components
     */
    private DiscoveryEventListener cameraDiscoveryListener = new DiscoveryEventListener() {
        @Override
        public void onCameraFound(Identity identity) {
            Log.d(TAG, "onCameraFound identity:" + identity);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cameraHandler.add(identity);
                    try{
                        Connect();
                    }catch (Exception e){
                        Log.d(TAG,"NO Connect");

                    }

                }
            });
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
            Log.d(TAG, "onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopDiscovery();
                    MainActivity.this.showMessage.show("onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);
                }
            });
        }
    };

    private ShowMessage showMessage = message -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

//    private void showSDKversion(String version) {
//        TextView sdkVersionTextView = findViewById(R.id.sdk_version);
//        String sdkVersionText = getString(R.string.sdk_version_text, version);
//        sdkVersionTextView.setText(sdkVersionText);
//    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        stopService(new Intent(MainActivity.this,MyService.class));
        return super.onTouchEvent(event);
    }

    private TextView bottomBar;

    private void setupViews() {
        tm=findViewById(R.id.temperatureText);
        tm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this,test_home.class);
                startActivity(i);
                finish();
            }
        });


//        descFlirOneStatus = findViewById(R.id.description);
//        graphicOverlay = findViewById(R.id.graphic_overlay);
//        msxImage = findViewById(R.id.msx_image);
//        dcimage=findViewById(R.id.msx_image1);
//        Bitmap myLogo = ((BitmapDrawable)getResources().getDrawable(R.drawable.navin)).getBitmap();
//        InputImage image = InputImage.fromBitmap(myLogo, 0)  ;
//        faceDetectors.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
//            @Override
//            public void onSuccess(List<Face> faces) {
//                if (faces.size()>=1){
//                    Face face=faces.get(0);
//                    Bitmap myLogo1=drawBackground(myLogo,new RectF(face.getBoundingBox()));
//                    dcimage.setImageBitmap(myLogo1);
//                }
//
//            }
//        }) ;




    }



    public String stringFourDigits(String str) {
        return str.length() < 4 ? str : str.substring(0, 4);
    }

    public  String stringTwoDigits(String str) {
        return str.length() < 2 ? str : str.substring(0, 2);
    }




}
