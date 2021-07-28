/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samples.flironecamera;

import android.Manifest;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Trace;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;


import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import com.samples.flironecamera.env.ImageUtils;
import com.samples.flironecamera.env.Logger;
public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
//  //add
//  private static final String TAG = "MainActivity";
//
//  //Handles Android permission for eg Network
//  private PermissionHandler permissionHandler;
//
//  //Handles network camera operations
//  private CameraHandler cameraHandler;
//
//  private Identity connectedIdentity = null;
//  private TextView connectionStatus;
//  private TextView discoveryStatus;
//
//  private ImageView msxImage;
//  private ImageView photoImage;
//
//  private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue(21);
//  private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();
//  public   String temperatureData = null;
//  public TextView temperature_Data;
public String temperatureData="000";

    public interface ShowMessage {
    void show(String message);
  }

  //add above
  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private boolean debug = false;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;

  public LinearLayout bottomSheetLayout;
  public LinearLayout gestureLayout;
  private BottomSheetBehavior<LinearLayout> sheetBehavior;

//  protected TextView frameValueTextView, cropValueTextView, inferenceTimeTextView;
  protected ImageView bottomSheetArrowImageView;
//  private ImageView plusImageView, minusImageView;
  private SwitchCompat apiSwitchCompat;
//  private TextView threadsTextView;

  private FloatingActionButton btnSwitchCam;
//  public  String temperatureData = "Please Connect to flirOne";

  private static final String KEY_USE_FACING = "use_facing";
  private Integer useFacing = null;
  private String cameraId = "Camera";

  protected Integer getCameraFacing() {
    return useFacing;
  }

  public TextView temperatureText;
  public PermissionHandler permissionHandler;
    private MainActivity.ShowMessage showMessage = message -> Toast.makeText(CameraActivity.this, message, Toast.LENGTH_SHORT).show();



  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
      setupView();
      permissionHandler = new PermissionHandler(showMessage, CameraActivity.this);
      startService(new Intent(CameraActivity.this,MyService.class));
  }
  //add
  Messenger mService;
    boolean mBound = false;
    final String TAG = "MainFragment";

    public CameraActivity() {
        // Required empty public constructor
    }

    MyReceiver myReceiver;
    @Override
    public void onStart() {
//        myReceiver = new test_home.MyReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(MyService2.MY_ACTION);
//        registerReceiver(myReceiver, intentFilter);
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyService.MY_ACTION);
        registerReceiver(myReceiver, intentFilter);

        //Start our own service
//        Intent i = new Intent(test_home.this,
//                MyService.class);
//        startService(i);
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(CameraActivity.this, MyService.class);
        CameraActivity.this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onStop() {
//        unregisterReceiver(myReceiver);
        super.onStop();
        // Unbind from the service
        unregisterReceiver(myReceiver);
        if (mBound) {
            CameraActivity.this.unbindService(mConnection);
            mBound = false;
        }
    }
    private ServiceConnection mConnection = new ServiceConnection() {


        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService=new Messenger(service);
            mBound = true;
            Log.v("mehi", "connected is true");

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
            Log.v("mehi", "Disconnected!");


        }
    };

    public void stop(){
        stopService(new Intent(CameraActivity.this,MyService.class));
        if (mBound) {
            CameraActivity.this.unbindService(mConnection);
            mBound = false;
        }
    }

    Handler handler1 = new Handler();
    Runnable runnable;
    int delay = 3*1000; //Delay for 15 seconds.  One second = 1000 milliseconds.

    //    CameraHandler cameraHandler= MyService.cameraHandler;
    private class MyReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            String datapassed = intent.getStringExtra("DATAPASSED");
            temperatureData=datapassed;
//            temperatureText.setText(datapassed);

//            if (Float.parseFloat(datapassed)>=33){
//                Intent i = new Intent(test_home.this,login.class);
//                startActivity(i);
//            }




        }
    }

  private void setupView(){
      try {
          Intent intent = getIntent();
//    useFacing = intent.getIntExtra(KEY_USE_FACING, CameraCharacteristics.LENS_FACING_FRONT);
          useFacing = intent.getIntExtra(KEY_USE_FACING, CameraCharacteristics.LENS_FACING_FRONT);

          getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

          setContentView(R.layout.tfe_od_activity_camera);
          Toolbar toolbar = findViewById(R.id.toolbar);
          temperatureText = findViewById(R.id.temperatureText);
          setSupportActionBar(toolbar);
          getSupportActionBar().setDisplayShowTitleEnabled(false);
          temperatureText.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {

                      gotoHome();



              }
          });


          if (hasPermission()) {
              setFragment();
          } else {
              requestPermission();
          }

//    threadsTextView = findViewById(R.id.threads);
//    plusImageView = findViewById(R.id.plus);
//    minusImageView = findViewById(R.id.minus);
//    apiSwitchCompat = findViewById(R.id.api_info_switch);
          bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
          gestureLayout = findViewById(R.id.gesture_layout);
          sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
          bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);

//    btnSwitchCam = findViewById(R.id.fab_switchcam);

          ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
          vto.addOnGlobalLayoutListener(
                  new ViewTreeObserver.OnGlobalLayoutListener() {
                      @Override
                      public void onGlobalLayout() {
                          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                              gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                          } else {
                              gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                          }
                          //                int width = bottomSheetLayout.getMeasuredWidth();
                          int height = gestureLayout.getMeasuredHeight();

                          sheetBehavior.setPeekHeight(height);
                      }
                  });
          sheetBehavior.setHideable(false);

          sheetBehavior.setBottomSheetCallback(
                  new BottomSheetBehavior.BottomSheetCallback() {
                      @Override
                      public void onStateChanged(@NonNull View bottomSheet, int newState) {
                          switch (newState) {
                              case BottomSheetBehavior.STATE_HIDDEN:
                                  break;
                              case BottomSheetBehavior.STATE_EXPANDED: {
                                  bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                              }
                              break;
                              case BottomSheetBehavior.STATE_COLLAPSED: {
                                  bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                              }
                              break;
                              case BottomSheetBehavior.STATE_DRAGGING:
                                  break;
                              case BottomSheetBehavior.STATE_SETTLING:
                                  bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                                  break;
                          }
                      }

                      @Override
                      public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                      }

                  });

//    frameValueTextView = findViewById(R.id.frame_info);
//    cropValueTextView = findViewById(R.id.crop_info);
//    inferenceTimeTextView = findViewById(R.id.inference_info);

//    apiSwitchCompat.setOnCheckedChangeListener(this);

//    plusImageView.setOnClickListener(this);
//    minusImageView.setOnClickListener(this);

//    btnSwitchCam.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View v) {
//        connect(cameraHandler.getFlirOne());
//      }
//    });
      }catch (Exception e){}
      Log.i("ccccc",cameraId.toString());

  }

//  private void onSwitchCamClick() {
//
//    switchCamera();
//
//  }

//  public void switchCamera() {
//
//    Intent intent = getIntent();
//
//    if (useFacing == CameraCharacteristics.LENS_FACING_FRONT) {
//      useFacing = CameraCharacteristics.LENS_FACING_BACK;
//    } else {
//      useFacing = CameraCharacteristics.LENS_FACING_FRONT;
//    }
//
//    intent.putExtra(KEY_USE_FACING, useFacing);
//    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//
//    restartWith(intent);
//
//  }

//  private void restartWith(Intent intent) {
//    finish();
//    overridePendingTransition(0, 0);
//    startActivity(intent);
//    overridePendingTransition(0, 0);
//  }

   public void gotoHome(){
       Intent i = new Intent(CameraActivity.this,test_home.class);
       startActivity(i);
       stop();
       finish();
   }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
//        Camera.Parameters param= camera.getParameters();
//        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
          try {
              Camera.Parameters parameters = camera.getParameters();
              parameters.set("s3d-prv-frame-layout", "none");
              parameters.set("s3d-cap-frame-layout", "none");
              parameters.set("iso", "auto");
              parameters.set("contrast", 100);
              parameters.set("brightness", 50);
              parameters.set("saturation", 100);
              parameters.set("sharpness", 100);
              parameters.setAntibanding("auto");
              parameters.setPictureFormat(ImageFormat.JPEG);
              parameters.set("jpeg-quality", 100);
              parameters.setPictureSize(800, 600);
              parameters.setRotation(180);
              camera.setDisplayOrientation(90);
              camera.setParameters(parameters);
          } catch (Exception e) {
              // cannot get camera or does not exist
          }
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        //rgbBytes = new int[previewWidth * previewHeight];
        //onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
          rgbBytes = new int[previewWidth * previewHeight];
          int rotation = 90;
          if (useFacing == CameraCharacteristics.LENS_FACING_FRONT) {
              rotation = 270;
          }
          onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), rotation);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
        new Runnable() {
          @Override
          public void run() {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
          }
        };

    postInferenceCallback =
        new Runnable() {
          @Override
          public void run() {
            camera.addCallbackBuffer(bytes);
            isProcessingFrame = false;
          }
        };
    processImage();
  }

  /** Callback for Camera2 API */

  public int Noface=0;
  @Override
  public void onImageAvailable(final ImageReader reader) {

//      if(Noface>=100){
//          Intent i = new Intent(CameraActivity.this,test_home.class);
//          startActivity(i);
//          stop();
//          finish();
//
//
//      }
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
          new Runnable() {
            @Override
            public void run() {
              ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
            }
          };

      postInferenceCallback =
          new Runnable() {
            @Override
            public void run() {
              image.close();
              isProcessingFrame = false;
            }
          };

      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

//  @Override
//  public synchronized void onStart() {
//    LOGGER.d("onStart " + this);
//    super.onStart();
//  }

  @Override
  public synchronized void onResume() {
//    LOGGER.d("onResume " + this);
      handler1.postDelayed( runnable = new Runnable() {
          public void run() {
              //do something
              Message msg = Message.obtain(null, MyService.MSG_SAY_HELLO, 0, 0);
              try {
//                    mService.send(msg);

//                Toast.makeText(test_home.this,MyService.cameraHandler.getInfo(),Toast.LENGTH_SHORT).show();

                  Log.v(TAG, "Message sent.");
              } catch ( Exception e) {
                  e.printStackTrace();
              }

              handler.postDelayed(runnable, delay);
          }
      }, delay);
    super.onResume();;
    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());


  }

  @Override
  public synchronized void onPause() {
      handler1.removeCallbacks(runnable);
      LOGGER.d("onPause " + this);


    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }
      super.onPause();

  }

//  @Override
//  public synchronized void onStop() {
//    LOGGER.d("onStop " + this);
//    super.onStop();
//
//  }
//
  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
      stopService(new Intent(CameraActivity.this,MyService.class));
//    super.onDestroy();
//    super.stopDiscovery();
//    cameraHandler.disconnect();

  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);

    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//      permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        setFragment();
      } else {
        requestPermission();

      }
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
            .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

   private String chooseCamera() {

        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {


            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);


                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                //final int facing =
                //(facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
//        if (!facing.equals(useFacing)) {
//          continue;
//        }

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (useFacing != null &&
                        facing != null &&
                        !facing.equals(useFacing)
                ) {
                    continue;
                }


                useCamera2API = (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                                || isHardwareLevelSupported(
                                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);


                LOGGER.i("Camera API lv2?: %s", useCamera2API);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return "1";

    }


 protected void setFragment() {

        this.cameraId = chooseCamera();

        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    CameraActivity.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;

        } else {

          int facing = (useFacing == CameraCharacteristics.LENS_FACING_BACK) ?
                          Camera.CameraInfo.CAMERA_FACING_BACK :
                          Camera.CameraInfo.CAMERA_FACING_FRONT;
            LegacyCameraConnectionFragment frag = new LegacyCameraConnectionFragment(this,
                    getLayoutId(),
                    getDesiredPreviewFrameSize(), facing);
            fragment = frag;

        }

        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    setUseNNAPI(isChecked);
    if (isChecked) apiSwitchCompat.setText("NNAPI");
    else apiSwitchCompat.setText("TFLITE");
  }

  @Override
  public void onClick(View v) {
//    if (v.getId() == R.id.plus) {
//      String threads = threadsTextView.getText().toString().trim();
//      int numThreads = Integer.parseInt(threads);
//      if (numThreads >= 9) return;
//      numThreads++;
//      threadsTextView.setText(String.valueOf(numThreads));
//      setNumThreads(numThreads);
//    } else if (v.getId() == R.id.minus) {
//      String threads = threadsTextView.getText().toString().trim();
//      int numThreads = Integer.parseInt(threads);
//      if (numThreads == 1) {
//        return;
//      }
//      numThreads--;
//      threadsTextView.setText(String.valueOf(numThreads));
//      setNumThreads(numThreads);
//    }
  }

  protected void showFrameInfo(String frameInfo) {
//    frameValueTextView.setText(frameInfo);
  }

  protected void showCropInfo(String cropInfo) {
//    cropValueTextView.setText(cropInfo);
  }

  protected void showInference(String inferenceTime) {
//    inferenceTimeTextView.setText(inferenceTime);
//      temperature_Data.setText("hello");
  }


  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void setNumThreads(int numThreads);

  protected abstract void setUseNNAPI(boolean isChecked);

}
