package com.samples.flironecamera;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.BuildConfig;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;
import com.google.mlkit.vision.face.FaceDetector;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class MyService extends Service {
    public MyService() {
    }
    MediaPlayer player;
    public PermissionHandler permissionHandler;
    private FaceDetector faceDetectors;
    private Handler handler ;
    private Runnable runnable;
    private ImageView dcimage;
    private Bitmap mymsx;
    private static final String TAG = "MainActivity2";


    //Handles network camera operations
    public static CameraHandler cameraHandler;

    public Identity connectedIdentity = null;
    //private TextView connectionStatus;
    //private TextView discoveryStatus;
    private TextView descFlirOneStatus;

//    private ImageView msxImage;
//    private ImageView photoImage;
    final static String MY_ACTION = "MY_ACTION";

    private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue(100);
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();

//    GraphicOverlay graphicOverlay;
//    public Bitmap mybitmap=null;

    public   String temperatureData ="0";

    private TextView tm;
    public int goToNextAction;

    public interface ShowMessage {
        void show(String message);
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try{
            ThermalLog.LogLevel enableLoggingInDebug;
            if (BuildConfig.DEBUG) enableLoggingInDebug = ThermalLog.LogLevel.DEBUG;
            else enableLoggingInDebug = ThermalLog.LogLevel.NONE;

            //ThermalSdkAndroid has to be initiated from a Activity with the Application Context to prevent leaking Context,
            // and before ANY using any ThermalSdkAndroid functions
            //ThermalLog will show log from the Thermal SDK in standards android log framework
            ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);
        }catch (Exception e){}

//        permissionHandler = new PermissionHandler(MyService.this);

        cameraHandler = new CameraHandler();




//        Bitmap myLogo = ((BitmapDrawable)getResources().getDrawable(R.drawable.logo)).getBitmap();
//        mybitmap=myLogo;
        this.startDiscovery();

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopDiscovery();
        this.disconnect();
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

    public void connect(Identity identity) {
        //We don't have to stop a discovery but it's nice to do if we have found the camera that we are looking for
        cameraHandler.stopDiscovery(discoveryStatusListener);

        if (connectedIdentity != null) {
            Log.d(TAG, "connect(), in *this* code sample we only support one camera connection at the time");
//            this.stopDiscovery();
//            this.disconnect();
//            showMessage.show("connect(), in *this* code sample we only support one camera connection at the time");
            return;
        }

        if (identity == null) {
            Log.d(TAG, "connect(), can't connect, no camera available");
//            showMessage.show("connect(), can't connect, no camera available");
//            MyService.MyThread myThread = new MyService.MyThread();
//            myThread.start();
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


    private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
        @Override
        public void permissionGranted(Identity identity) {
            doConnect(identity);
        }

        @Override
        public void permissionDenied(Identity identity) {
            Log.d(TAG,"permss");

//            MainActivity2.this.showMessage.show("Permission was denied for identity ");
        }

        @Override
        public void error(UsbPermissionHandler.UsbPermissionListener.ErrorType errorType, final Identity identity) {
//            MainActivity2.this.showMessage.show("Error when asking for permission for FLIR ONE, error:"+errorType+ " identity:" +identity);
        }
    };

    private void doConnect(Identity identity) {
        new Thread(() -> {
            this.startDiscovery();
            try {
                cameraHandler.connect(identity, connectionStatusListener);

                    updateConnectionText(identity, "CONNECTED");
                    cameraHandler.startStream(streamDataListener);

            } catch (IOException e) {

                    Log.d(TAG, "Could not connect: " + e);
                    updateConnectionText(identity, "DISCONNECTED");
//                    descFlirOneStatus.setText("");

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

                updateConnectionText(null, "DISCONNECTED");
                connectedIdentity=null;
//                    descFlirOneStatus.setText("");

        }).start();
        temperatureData="0";

    }

    /**
     * Update the UI text for connection status
     */


    private void updateConnectionText(Identity identity, String status) {
        String deviceId = identity != null ? identity.deviceId : "";
        Log.i(TAG,status);
//        MainActivity2.this.showMessage.show(deviceId + ": " + status);
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

        }

        @Override
        public void stopped() {

        }
    };

    /**
     * Camera connecting state thermalImageStreamListener, keeps track of if the camera is connected or not
     * <p>
w UI components
     */
    private ConnectionStatusListener connectionStatusListener = errorCode -> {
        Log.d(TAG, "onDisconnected errorCode:" + errorCode);

       updateConnectionText(connectedIdentity, "DISCONNECTED");
    };

    private final CameraHandler.StreamDataListener streamDataListener = new CameraHandler.StreamDataListener() {

        @Override
        public void images(FrameDataHolder dataHolder) {


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


//                Log.d(TAG,"framebuffer size:"+framesBuffer.size());
//                Log.d(TAG,temperatureData);

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

//                Toast.makeText(MainActivity2.this,temperatureData,Toast.LENGTH_LONG).show();
//                temperatureData=cameraHandler.getInfo();
            if (Float.parseFloat(cameraHandler.getInfo())>29){
                MyService.MyThread myThread = new MyService.MyThread();
                myThread.start();

            }

//                tm.setText(temperatureData);

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

        }

    };




    /**
     * Camera Discovery thermalImageStreamListener, is notified if a new camera was found during a active discovery phase
     * <p>

     */
    private DiscoveryEventListener cameraDiscoveryListener = new DiscoveryEventListener() {
        @Override
        public void onCameraFound(Identity identity) {
            Log.d(TAG, "onCameraFound identity:" + identity);

                    cameraHandler.add(identity);
                    try{
                        Connect();
                    }catch (Exception e){
                        Log.d(TAG,"NO Connect");

                    }



        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {

                    stopDiscovery();

        }
    };

//    private MainActivity.ShowMessage showMessage = message -> Toast.makeText(MainActivity2.this, message, Toast.LENGTH_SHORT).show();

//    private void showSDKversion(String version) {
//        TextView sdkVersionTextView = findViewById(R.id.sdk_version);
//        String sdkVersionText = getString(R.string.sdk_version_text, version);
//        sdkVersionTextView.setText(sdkVersionText);
//    }







    public String stringFourDigits(String str) {
        return str.length() < 4 ? str : str.substring(0, 4);
    }

    public  String stringTwoDigits(String str) {
        return str.length() < 2 ? str : str.substring(0, 2);
    }

    /**
     * Command to the service to display a message
     */
    static  final int MSG_SAY_HELLO = 1;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.v("MsgService", "Message received");
            switch (msg.what) {
                case MSG_SAY_HELLO:
                    Toast.makeText(getApplicationContext(), cameraHandler.getInfo(), Toast.LENGTH_SHORT).show();
//                    Intent n= new Intent(getApplicationContext(),MainActivity.class);
                    
//                    startActivity(n);

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */



    // to send data to activity

    public class MyThread extends Thread{

        @Override
        public void run() {
            // TODO Auto-generated method stub


//                    Thread.sleep(5000);
            Intent intent = new Intent();
            intent.setAction(MY_ACTION);

            intent.putExtra("DATAPASSED", cameraHandler.getInfo());

            sendBroadcast(intent);


            stopSelf();
        }

    }


}
