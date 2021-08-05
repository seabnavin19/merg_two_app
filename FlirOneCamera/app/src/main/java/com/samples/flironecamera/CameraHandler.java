/*******************************************************************
 * @title FLIR THERMAL SDK
 * @file CameraHandler.java
 * @Author FLIR Systems AB
 *
 * @brief Helper class that encapsulates *most* interactions with a FLIR ONE camera
 *
 * Copyright 2019:    FLIR Systems
 ********************************************************************/
package com.samples.flironecamera;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import androidx.annotation.RequiresApi;

import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.image.Point;
import com.flir.thermalsdk.image.Rectangle;
import com.flir.thermalsdk.image.TemperatureUnit;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.image.palettes.Palette;
import com.flir.thermalsdk.image.palettes.PaletteManager;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.ConnectParameters;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.streaming.ThermalImageStreamListener;


import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the handling of a FLIR ONE camera or built in emulator, discovery, connecting and start receiving images.
 * All listeners are called from Thermal SDK on a non-ui thread
 * <p/>
 * Usage:
 * <pre>
 * Start discovery of FLIR FLIR ONE cameras or built in FLIR ONE cameras emulators
 * {@linkplain #startDiscovery(DiscoveryEventListener, DiscoveryStatus)}
 * Use a discovered Camera {@linkplain Identity} and connect to the Camera
 * (note that calling connect is blocking and it is mandatory to call this function from a background thread):
 * {@linkplain #connect(Identity, ConnectionStatusListener)}
 * Once connected to a camera
 * {@linkplain #startStream(StreamDataListener)}
 * </pre>
 * <p/>
 * You don't *have* to specify your application to listen or USB intents but it might be beneficial for you application,
 * we are enumerating the USB devices during the discovery process which eliminates the need to listen for USB intents.
 * See the Android documentation about USB Host mode for more information
 * <p/>
 * Please note, this is <b>NOT</b> production quality code, error handling has been kept to a minimum to keep the code as clear and concise as possible
 */
class CameraHandler {

    private static final String TAG = "CameraHandler";
    private String tempData = "0";
    private StreamDataListener streamDataListener;
    private String Info;
    private int go=0;
    private Point point=null;
    private Rectangle rectangle=null;
    private int width=0;
    private ArrayList<Double> temperatures = new ArrayList<>();
    Bitmap dcBitmap1;
    public interface StreamDataListener {
        void images(FrameDataHolder dataHolder);
        void images(Bitmap msxBitmap, Bitmap dcBitmap);
    }

    //Discovered FLIR cameras
    LinkedList<Identity> foundCameraIdentities = new LinkedList<>();

    //A FLIR Camera
    private Camera camera;
    private Bitmap imgBitmap;

    public void setWidth_height(Point point){
        this.point=point;

    }
    public void setRectangle(Rectangle rectangle){
        this.rectangle=rectangle;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public Point getPoint() {
        return point;
    }

    public interface DiscoveryStatus {
        void started();
        void stopped();
    }

    public CameraHandler() {
    }


    /**
     * Start discovery of USB and Emulators
     */
    public void startDiscovery(DiscoveryEventListener cameraDiscoveryListener, DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().scan(cameraDiscoveryListener, CommunicationInterface.EMULATOR, CommunicationInterface.USB);
        discoveryStatus.started();
    }

    /**
     * Stop discovery of USB and Emulators
     */
    public void stopDiscovery(DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().stop(CommunicationInterface.EMULATOR, CommunicationInterface.USB);
        discoveryStatus.stopped();
    }

    public void connect(Identity identity, ConnectionStatusListener connectionStatusListener) throws IOException {
        camera = new Camera();
//        camera.connect(identity, connectionStatusListener);
        camera.connect(identity,connectionStatusListener,new ConnectParameters());
    }

    public void disconnect() {
        if (camera == null) {
            return;
        }
        if (camera.isGrabbing()) {
            camera.unsubscribeAllStreams();
        }
        camera.disconnect();

    }

    /**
     * Start a stream of {@link ThermalImage}s images from a FLIR ONE or emulator
     */
    public void startStream(StreamDataListener listener) {
        this.streamDataListener = listener;
        camera.subscribeStream(thermalImageStreamListener);
    }

    public String getLogData() {
        if(this.tempData.isEmpty()) {
            return null;
        }
        return this.tempData;
    }

    public void setImg(Bitmap img) {
        this.imgBitmap = img;
    }

    public  Bitmap getImg() {
        return this.imgBitmap;
    }
    /**
     * Stop a stream of {@link ThermalImage}s images from a FLIR ONE or emulator
     */
    public void stopStream(ThermalImageStreamListener listener) {
        camera.unsubscribeStream(listener);
    }

    /**
     * Add a found camera to the list of known cameras
     */
    public void add(Identity identity) {
        foundCameraIdentities.add(identity);
    }

    @Nullable
    public Identity get(int i) {
        return foundCameraIdentities.get(i);
    }

    /**
     * Get a read only list of all found cameras
     */
    @Nullable
    public List<Identity> getCameraList() {
        return Collections.unmodifiableList(foundCameraIdentities);
    }

    /**
     * Clear all known network cameras
     */
    public void clear() {
        foundCameraIdentities.clear();
    }

    @Nullable
    public Identity getCppEmulator() {
        for (Identity foundCameraIdentity : foundCameraIdentities) {
            if (foundCameraIdentity.deviceId.contains("C++ Emulator")) {
                return foundCameraIdentity;
            }
        }
        return null;
    }

    @Nullable
    public Identity getFlirOneEmulator() {
        for (Identity foundCameraIdentity : foundCameraIdentities) {
            if (foundCameraIdentity.deviceId.contains("EMULATED FLIR ONE")) {
                return foundCameraIdentity;
            }
        }
        return null;
    }

    public String getInfo() {
        return tempData;
    }

    @Nullable
    public Identity getFlirOne() {
        for (Identity foundCameraIdentity : foundCameraIdentities) {
            boolean isFlirOneEmulator = foundCameraIdentity.deviceId.contains("EMULATED FLIR ONE");
            boolean isCppEmulator = foundCameraIdentity.deviceId.contains("C++ Emulator");
            if (!isFlirOneEmulator && !isCppEmulator) {
                return foundCameraIdentity;
            }
        }

        return null;
    }

    private void withImage(ThermalImageStreamListener listener, Camera.Consumer<ThermalImage> functionToRun) {
        camera.withImage(listener,functionToRun);
    }


    /**
     * Called whenever there is a new Thermal Image available, should be used in conjunction with {@link Camera.Consumer}
     */
    private final ThermalImageStreamListener thermalImageStreamListener = new ThermalImageStreamListener() {
        @Override
        public void onImageReceived() {
            //Will be called on a non-ui thread
            Log.d(TAG, "onImageReceived(), we got another ThermalImage");
            withImage(this, handleIncomingImage);
        }
    };

    /**
     * Function to process a Thermal Image and update UI

     */
    int k=0;
    private final Camera.Consumer<ThermalImage> handleIncomingImage = new Camera.Consumer<ThermalImage>() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void accept(ThermalImage thermalImage) {
            Log.d(TAG, "accept() called with: thermalImage = [" + thermalImage.getDescription() + "]");
            //Will be called on a non-ui thread,
            // extract information on the background thread and send the specific information to the UI thread

            //Get a bitmap with only IR data
            Bitmap msxBitmap;
            {
                Objects.requireNonNull(thermalImage.getFusion()).setFusionMode(FusionMode.THERMAL_ONLY);
                thermalImage.getFusion().setThermalFusionAbove(new ThermalValue(35.6, TemperatureUnit.CELSIUS));
                thermalImage.getFusion().setThermalFusionBelow(new ThermalValue(40, TemperatureUnit.CELSIUS));

                Palette palette = PaletteManager.getDefaultPalettes().get(0);

                thermalImage.setPalette(palette);
                thermalImage.setTemperatureUnit(TemperatureUnit.CELSIUS);
                ////////////////////////////
                // HERE!!!!!!  Get temperature at the center of thermal image
                tempData="0";


                if (getRectangle()==null){

                    try {

                        Double dblSpotTemperature = thermalImage.getValueAt(new Point((thermalImage.getWidth() / 2)-(thermalImage.getWidth()/15), thermalImage.getHeight() / 30));
                        dblSpotTemperature=Math.max( dblSpotTemperature,thermalImage.getValueAt(new Point((thermalImage.getWidth() / 2), thermalImage.getHeight() / 30)));
                        dblSpotTemperature=Math.max( dblSpotTemperature,thermalImage.getValueAt(new Point((thermalImage.getWidth() / 2), thermalImage.getHeight() /2)));
                        tempData=stringFourDigits(dblSpotTemperature);
                        Info=null;
                    }catch (Exception e){
                        Info="0";
                        tempData="0";
                    }


                }
                else {
                    tempData="0";

//                    Double dblSpotTemperature = thermalImage.getValueAt(new Point(thermalImage.getWidth() / 2, thermalImage.getHeight() / 2));
//                    tempData=stringFourDigits(dblSpotTemperature);
                    Info=null;
                }


//                Log.d("++",String.valueOf((int) (thermalImage.getHeight()-0.001))+" "+thermalImage.getHeight());
//                Double dblSpotTemperature = thermalImage.getValueAt(new Point(thermalImage.getWidth() / 2, thermalImage.getHeight() / 2));
//                Log.d("+++++++", String.valueOf(dblSpotTemperature));
                msxBitmap = BitmapAndroid.createBitmap(thermalImage.getImage()).getBitMap();
//                tempData = String.valueOf(dblSpotTemperature);
                //onImageProcess(msxBitmap, tempData);
//                setImg(msxBitmap);
            }
            //Get a bitmap with the visual image, it might have different dimensions then the bitmap from THERMAL_ONLY
            Bitmap dcBitmap = BitmapAndroid.createBitmap(Objects.requireNonNull(thermalImage.getFusion().getPhoto())).getBitMap();
//
//
            msxBitmap=Bitmap.createScaledBitmap(msxBitmap,thermalImage.getWidth(),thermalImage.getHeight(),false);
            streamDataListener.images(msxBitmap,dcBitmap);
//            tempData=thermalImage.getWidth()+" "+ dcBitmap.getWidth();

        }
    };

    public String stringFourDigits(Double s) {
        String str=String.valueOf(s);
        return str.length() < 4 ? str : str.substring(0, 4);
    }



}