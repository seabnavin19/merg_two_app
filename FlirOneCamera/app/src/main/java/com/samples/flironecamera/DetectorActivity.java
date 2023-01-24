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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpEntity;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpPost;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.samples.flironecamera.customview.OverlayView;
import com.samples.flironecamera.customview.OverlayView.DrawCallback;
import com.samples.flironecamera.env.BorderedText;
import com.samples.flironecamera.env.ImageUtils;
import com.samples.flironecamera.env.Logger;
import com.samples.flironecamera.tflite.SimilarityClassifier;
import com.samples.flironecamera.tflite.TFLiteObjectDetectionAPIModel;
import com.samples.flironecamera.tracking.ApiClient;
import com.samples.flironecamera.tracking.AttendanceClient;
import com.samples.flironecamera.tracking.MultiBoxTracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener, MainActivity {
    private static final Logger LOGGER = new Logger();


    // FaceNet
    private static final int TF_OD_API_INPUT_SIZE = 160;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "facenet.tflite";


    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);


    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private SimilarityClassifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;
    private boolean addPending = false;
    //private boolean adding = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;

    // Face detector
    private FaceDetector faceDetector;

    // here the preview image is drawn in portrait way
    private Bitmap portraitBmp = null;
    // here the face is cropped and drawn
    private Bitmap faceBmp = null;


    private Button fabAdd;
    public TextView framText;
    private String NameFromFirebase;
    private Button connectButton;
    private Button disconnectButton;
    private FloatingActionButton backButton;
    private ProgressDialog progressDialog;
    private HashSet<String> IdFace;
    private ArrayList<String> temperatures;
    private Boolean Allow_FaceDetect = true;
    private int AddedFace = 0;
    private HashMap<String, String> resultMap;
    //  private  int Noface;
    private String ID;
    private String currentuser;
    public int Attendance;
    private MediaPlayer mp;
    private MediaPlayer alert;
    private String temperatureData="0";

    //to hide this 2 button
    private Button attendance;
    private Button add_people;


    //per location
    public String location;
    private String location_txt = "three";
    private FirebaseFirestore db;
    private Object MutableLiveData;

    private Float temporary = 0f;
//  private TextView faceLocation;
    private List<Map<String, Object>> AllLocation = new ArrayList<>();

    //private HashMap<String, Classifier.Recognition> knownFaces = new HashMap<>();

    //use this variable to store all the id of people inorder to avoid adding duplicate people with the same id
    private ArrayList<String> Ids = new ArrayList<>();

    public String stringFourDigits(String str) {
        return str.length() < 4 ? str : str.substring(0, 4);
    }

    public String stringTwoDigits(String str) {
        return str.length() < 2 ? str : str.substring(0, 2);
    }


    //list of face fetch from database
    private List<Map<String, Object>> AllFaceFromDataBase = new ArrayList<>();
    private Button addButton;
    private Spinner dropdown;
    private ArrayList<String> items;
    private TextView emailText;

    private ArrayList<String> em = new ArrayList<>();


    int take = 1;
    int Noattendance = 0;
    int check = 0;
    int prepare = 0;
    private HashMap<String, String> userIDFace = new HashMap<>();

    //store all the face that already take within 5 min
    private ArrayList<String> AlreadyTake = new ArrayList<>();


    //to fetch face
    private fetch_face fetch_faces;

    //  private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
//      final Intent[] i = {getIntent()};
            String email = "User";
            if (currentuser != null) {
                email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            }
            getLocationNames();
            emailText = findViewById(R.id.showEmail);
            emailText.setText(email);
            currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid();


            //register all location from firebase to selector
            ArrayList<String> items = new ArrayList<>();
            dropdown = findViewById(R.id.spinner1);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, items);
        db = FirebaseFirestore.getInstance();
        db.collection(currentuser).document("Location").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
          @Override
          public void onSuccess(DocumentSnapshot documentSnapshot) {
            Map<String, Object> locationMap = documentSnapshot.getData();
            if (documentSnapshot.exists()) {

              for (Map.Entry<String, Object> location_name : locationMap.entrySet()) {
                Map<String, Object> loc = (Map<String, Object>) location_name.getValue();
                items.add(loc.get("name").toString());
              }
              ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, items);

              dropdown.setAdapter(adapter);
            } else {
              String[] SpinnerArray = new String[]{"NoLocation"};

              ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, SpinnerArray);


            }


          }
        });
        db.collection(currentuser).document("Email").get().addOnSuccessListener(documentSnapshot -> {

          if (documentSnapshot.exists()) {

            Map<String, Object> emails = documentSnapshot.getData();
            for (Map.Entry<String, Object> user_name : emails.entrySet()) {
              Map<String, Object> mail = (Map<String, Object>) user_name.getValue();
              em.add(mail.get("email").toString());
            }
            successToast(temperatureData);
            successToast("Registered Face");
          }

        });
            emailText.setText(email);
//


            dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    location_txt = dropdown.getSelectedItem().toString();
//          successToast("mother");
//            RegisterFaceFromFireBase();
                    // change api
                    Fetch_Detail_From_Api();

                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        check = 1;
                        LoadFaceFromFirebase();
                        successToast(String.valueOf(AllFaceFromDataBase.size()));
                        tracker.setIdname(userIDFace);
                    }, 6000);

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
//          LoadFaceFromFirebase();

                }
            });

            Attendance = 1;
            Log.d("user", currentuser);
            mp = MediaPlayer.create(this, R.raw.temprature_checked);
            alert = MediaPlayer.create(this, R.raw.high);
            fabAdd = findViewById(R.id.fab_add);

            connectButton = findViewById(R.id.connect_to_flir);
            disconnectButton = findViewById(R.id.disconnect_flir);
            backButton = findViewById(R.id.ChangeAttendance);
            attendance = findViewById(R.id.text_attendance);
            add_people = findViewById(R.id.GotoAddFace);

            //
//    location= findViewById(R.id.location);


            if (Attendance == 0) {
                add_people.setVisibility(View.INVISIBLE);
                attendance.setVisibility(View.INVISIBLE);
            }

            IdFace = new HashSet<>();
            temperatures = new ArrayList<>();

            fabAdd.setOnClickListener(view -> onAddClick());

            // Real-time contour detection of multiple faces
            FaceDetectorOptions options =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                            .build();

            FaceDetector detector = FaceDetection.getClient(options);

            faceDetector = detector;
        } catch (Exception e) {
            Log.d("ex", e.toString());
        }


    }


    private void onAddClick() {

        addPending = true;

    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());

        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);
        tracker = new MultiBoxTracker(this);


        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            //cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {

            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();

        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);


        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        } else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Config.ARGB_8888);

        portraitBmp = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);


        Matrix frameToPortraitTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        targetW, targetH,
                        sensorOrientation, MAINTAIN_ASPECT);


        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    int detect = 0;

    @Override
    protected void processImage() {

        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();


        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        // converting bitmap object to YUV_420_888 format



        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }
        Log.i("temmm", temperatureData);


        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);


        faceDetector
                .process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 0) {
                        updateResults(currTimestamp, new LinkedList<>());
                        temperatureText.setText("");
                        Noface += 1;
                        IdFace.clear();
                        temperatures.clear();
                        temperatureData = "0";
                        temporary = 0f;

                        // 500 equal to 1 minute
                        // go to home page after no face detected  15 min
                        if (Noface >= 500 * 15 && k == 0) {

                            k = 1;
                            gotoHome();
                        }
                        return;

                    } else {

                        if (Float.parseFloat(temperatureData) > 29) {

                            if (Float.parseFloat(temperatureData) >= temporary) {

                                temperatureText.setText(temperatureData + " °C");
                                temporary = Float.parseFloat(temperatureData);

                            } else {
                                temperatureText.setText((temporary) + " °C");
                            }

                        } else {
                            temperatureText.setText("");
                        }


                    }
                    runInBackground(
                            new Runnable() {
                                @Override
                                public void run() {

                                    onFacesDetected(currTimestamp, faces, addPending);
                                    addPending = false;


                                }
                            });
                });
    }

    int k = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }


    // Face Processing
    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;

    }


    public SimilarityClassifier.Recognition NewPerson;


    //this dialog will alert when use add face
    // it will ask to input email

    private void showAddFaceDialog(SimilarityClassifier.Recognition rec) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.image_edit_dialog, null);
        ImageView ivFace = dialogLayout.findViewById(R.id.dlg_image);
        TextView tvTitle = dialogLayout.findViewById(R.id.dlg_title);
//    EditText etName = dialogLayout.findViewById(R.id.dlg_input);
        EditText id = dialogLayout.findViewById(R.id.id_input);

        tvTitle.setText("Add Face");
        rec.setColor(1);
        ivFace.setImageBitmap(rec.getCrop());
        NewPerson = rec;
//    etName.setHint("Enter name");
        id.setHint("Enter Email Address");
        builder.setNegativeButton("Cancel", (dialog, which) -> {

        });


        // add face to database by just enter the email

        builder.setPositiveButton("OK", (dlg, i) -> {
            String name;
            String email = id.getText().toString();

            if (email.isEmpty()) {
                return;
            }

            email = email.replaceAll("\\.", "");
            name = getNameFromEmail(email);
            NameFromFirebase = name;
            ID = email;

            if (name.isEmpty() || email.isEmpty()) {
                return;
            }

            HashMap<String, Object> user = new HashMap<>();

            user.put("Id", rec.getId());
            user.put("Distance", rec.getDistance());
            user.put("Title", rec.getTitle());


            //need to change to email
            user.put("Name", name);
            user.put("ID", email);
            user.put("Extra", rec.getExtra().toString());


            userIDFace.put(email, name);

            builder.setCancelable(false);

            progressDialog = new ProgressDialog(DetectorActivity.this);
            String reques = "{\"name\": \"" + name + "\", \"email\": \"" + ID + "\", \"faceString\": \"" + rec.getExtra().toString() + "\"}";
            System.out.println(reques);
            progressDialog.setMessage("Uploading Image To DataBase..!");
            progressDialog.show();
            AddNewFace();
            dlg.dismiss();
        });


        builder.setView(dialogLayout);
        builder.setCancelable(false);
        builder.show();


    }


    private String getNameFromEmail(String email) {
        String leftPart = email.substring(0, email.indexOf("@"));


        return leftPart;

    }


    private void updateResults(long currTimestamp, final List<SimilarityClassifier.Recognition> mappedRecognitions) {

        tracker.trackResults(mappedRecognitions, currTimestamp);
        trackingOverlay.postInvalidate();
        computingDetection = false;
        //adding = false;

        if (mappedRecognitions.size() > 0) {
            LOGGER.i("Adding results");
            SimilarityClassifier.Recognition rec = mappedRecognitions.get(0);
            if (rec.getExtra() != null) {
                showAddFaceDialog(rec);
//         framText.setText(rec.getCrop().getClass().getName().toString());
            }

        }

        runOnUiThread(
                () -> {

                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());
                    showInference(lastProcessingTimeMs + "ms");
                });

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Thread.currentThread().interrupt();

        updateResults(0, new LinkedList<>());
        temperatureText.setText("");
        Noface += 1;
        IdFace.clear();
        temperatures.clear();
        temperatureData = "0";
        temporary = 0f;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                LoadFaceFromFirebase();
                check = 1;
                tracker.setIdname(userIDFace);

            }
        }, 3000);


    }

    @Override
    public synchronized void onResume() {
        super.onResume();
//    updateResults(currTime, facees);;
    }

    private void onFacesDetected(long currTimestamp, List<Face> faces, boolean add) {
//    detectFace();
        if (Float.parseFloat(temperatureData) >= 0) {
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
                case TF_OD_API:
                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    break;
            }

            final List<SimilarityClassifier.Recognition> mappedRecognitions =
                    new LinkedList<SimilarityClassifier.Recognition>();


            //final List<Classifier.Recognition> results = new ArrayList<>();

            // Note this can be done only once
            int sourceW = rgbFrameBitmap.getWidth();
            int sourceH = rgbFrameBitmap.getHeight();
            int targetW = portraitBmp.getWidth();
            int targetH = portraitBmp.getHeight();
            Matrix transform = createTransform(
                    sourceW,
                    sourceH,
                    targetW,
                    targetH,
                    sensorOrientation);
            final Canvas cv = new Canvas(portraitBmp);

            // draws the original image in portrait mode.
            cv.drawBitmap(rgbFrameBitmap, transform, null);

            final Canvas cvFace = new Canvas(faceBmp);
            Face facea = faces.get(0);
//      detectFace();
            final RectF boundingBoxt = new RectF(facea.getBoundingBox());
            if (take == 1) {

                if (temperatureData == null) {
                    temperatureData = "0";
                }

                if (Float.parseFloat(temperatureData) >= 35) {
                    temperatures.add(temperatureData);
                }

                //      Log.d("kkkk",String.valueOf(temperatures.size()));
            }

            if (Attendance == 0 || Noattendance == 1) {

                if (Noattendance == 0) {
                    temperatures.clear();
                }

                if (temperatures.size() >= 2 && check == 1) {
                    take = 0;
                    IdFace.clear();
                    try {
                        TemperatureDialog();
                    } catch (Exception e) {
                        take = 1;
                        check = 1;
                        temperatures.clear();
                        Noattendance = 0;
                        prepare = 0;
                        temporary = 0f;
                    }

                    prepare = 0;
                }
            }
            ArrayList<Face> faces1 = new ArrayList<>();
            faces1.add(faces.get(0));

            for (Face face : faces1) {

                LOGGER.i("Running detection on face " + currTimestamp);
                //results = detector.recognizeImage(croppedBitmap);

                final RectF boundingBox = new RectF(face.getBoundingBox());

                //final boolean goodConfidence = result.getConfidence() >= minimumConfidence;
                final boolean goodConfidence = true; //face.get;
                if (boundingBox != null && goodConfidence) {

                    // maps crop coordinates to original
                    cropToFrameTransform.mapRect(boundingBox);

                    // maps original coordinates to portrait coordinates
                    RectF faceBB = new RectF(boundingBox);
                    transform.mapRect(faceBB);

                    // translates portrait to origin and scales to fit input inference size
                    //cv.drawRect(faceBB, paint);
                    float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                    float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                    Matrix matrix = new Matrix();
                    matrix.postTranslate(-faceBB.left, -faceBB.top);
                    matrix.postScale(sx, sy);

                    cvFace.drawBitmap(portraitBmp, matrix, null);

                    //canvas.drawRect(faceBB, paint);

                    String label = "";
                    float confidence = -1f;
                    Integer color = Color.BLUE;
                    float[][] extra = null;
                    Bitmap crop = null;

                    if (add) {

                        crop = Bitmap.createBitmap(portraitBmp,
                                (int) faceBB.left,
                                (int) faceBB.top,
                                (int) faceBB.width(),
                                (int) faceBB.height());
                    }

                    final long startTime = SystemClock.uptimeMillis();
                    final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, add);
                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                    if (resultsAux.size() > 0) {

                        SimilarityClassifier.Recognition result = resultsAux.get(0);

                        extra = result.getExtra();

                        float conf = result.getDistance();
                        if (conf < 0.69f) {
                            confidence = conf;
                            label = result.getTitle();
                            if (result.getId().equals("0")) {
                                Noattendance = 0;
                                color = Color.GREEN;

                                //              Float temp= Float.parseFloat(temperatureData);

                                if (temperatureData == null) {
                                    temperatureData = "0";
                                }
                                //              Float temp=Float.parseFloat(temperatureData);
                                if (Float.parseFloat(temperatureData) >= 35) {
                                    temperatures.add(temperatureData);
                                }

                                Check();

//                Log.d("MyFace", String.valueOf(AddedFace));

                            } else {
                                color = Color.RED;
                            }
                        } else {
                            prepare += 1;

//              Log.d("kkkk", "pre" + String.valueOf(prepare));

                            if (prepare >= 5) {
                                Noattendance = 1;
                                prepare = 0;
                            }
                        }

                    }

                    if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {

                        // camera is frontal so the image is flipped horizontally
                        // flips horizontally
                        Matrix flip = new Matrix();
                        if (sensorOrientation == 90 || sensorOrientation == 270) {
                            flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                        } else {
                            flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                        }
                        //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                        flip.mapRect(boundingBox);

                    }

                    final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                            "0", label, confidence, boundingBox);

                    result.setColor(color);
                    result.setLocation(boundingBox);
                    result.setExtra(extra);
                    result.setCrop(crop);
                    mappedRecognitions.add(result);

                }


            }


            updateResults(currTimestamp, mappedRecognitions);

        } else {
            updateResults(0, new LinkedList<>());
            temperatureData = "0";
        }

    }

    //Dialog for user to decide to have attendance checking or not
    public void AskingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.askingoption, null);

        builder.setPositiveButton("Ok", (dialog, which) -> {
            check = 1;
            LoadFaceFromFirebase();
            tracker.setIdname(userIDFace);
        });

        builder.setCancelable(false);
        builder.setNegativeButton("No Thanks", (dialog, which) -> check = 1);
        builder.setView(dialogLayout);
        builder.show();
    }


    public void FetchAllFaceFromFirebase(View view) {
        check = 1;
        LoadFaceFromFirebase();
        tracker.setIdname(userIDFace);
    }


    public void LoadFaceFromFirebase() {
        Ids.clear();
        for (Map<String, Object> document : AllFaceFromDataBase) {
            float[][] Extra = new float[1][];
            String[] string_face = (String[]) document.get("faceString");

            float[] arr0 = new float[string_face.length];
            String Name = document.get("email").toString();
//      Ids.clear();
            Ids.add(Name);
            for (int k = 0; k < string_face.length; k++) {

                arr0[k] = Float.parseFloat(String.valueOf(string_face[k]));


            }

            Extra[0] = arr0;

            SimilarityClassifier.Recognition Newface = new SimilarityClassifier.
                    Recognition(
                    document.get("email").toString(),
                    document.get("name").toString(),
                    (float) -1,
                    new RectF());

            Newface.setExtra(Extra);
            detector.register(Name, Newface);

        }
        Toast.makeText(DetectorActivity.this, "Sucess register", Toast.LENGTH_LONG).show();
    }

    public String ChangeFaceArrayToStringArray(float[][] Array_face) {

        String string_face = "";
        float[] ind1 = Array_face[0];

        string_face += ind1[0];
        for (int i = 1; i <= ind1.length - 1; i++) {
            string_face += "," + ind1[i];

        }

        return string_face;
    }


    // to send face to firebase if the id is not duplicate

    public void SendData() {
        Map<String, Object> user = new HashMap<>();
        float[][] n = NewPerson.getExtra();

        String string_n = ChangeFaceArrayToStringArray(n);

        // check and change for new api
        user.put("name", NameFromFirebase);
        user.put("email", ID);
        user.put("faceString", string_n.toString().split(","));


        AllFaceFromDataBase.add(user);

        String name = NameFromFirebase;
        String email = ID;
        String faceString = string_n.substring(1, string_n.length() - 1);


        Map<String, String> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("faceString", faceString);

        ApiClient apiClient = new ApiClient(getApplicationContext());
        apiClient.sendData(data);
        progressDialog.dismiss();
    }


    public void AddNewFace() {
        if (!Ids.contains(ID)) {
            SendData();
        } else {
            progressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This ID already Contain in Database Do you want to Update?");
            builder.setCancelable(false)
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            progressDialog.show();
                            SendData();
                        }
                    });
            builder.show();
        }


    }

    private RequestQueue mQueue;

    public void Fetch_Detail_From_Api() {

        List<Map<String, Object>> usersReplace = new ArrayList<>();


        //fetch from api
        mQueue = Volley.newRequestQueue(DetectorActivity.this);
        String url = "https://rest-attend-api-production.up.railway.app/users";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {

                    for (int i = 0; i < response.length(); i++) {
                        HashMap<String, Object> user = new HashMap<>();
//            float [] face= new float[512];
                        JSONObject jsonObject = response.getJSONObject(i);
                        String name = jsonObject.getString("name");
                        String email = jsonObject.getString("email");

                        String[] face = jsonObject.getString("faceString").split(",");

                        user.put("email", email);
                        user.put("name", name);
                        user.put("faceString", face);

                        userIDFace.put(email, name);
                        usersReplace.add(user);
                    }


                    AllFaceFromDataBase = usersReplace;

//          people_face_array student = new people_face_array("0", name, 1, , "");
//          Toast.makeText(DetectorActivity.this, "" + id, Toast.LENGTH_LONG).show();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(DetectorActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mQueue.add(request);
    }


    //private ImageView im;
    public void GotoAddFace(View view) {
        // condition to stop detect the face

        check = 0;
        take = 0;
        check = 0;
        temperatures.clear();
        Noattendance = 0;
        prepare = 0;

        //visible all button in order to add face

        fabAdd.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        bottomSheetLayout.setVisibility(View.INVISIBLE);
        detector.unregister();
        connectButton.setVisibility(View.INVISIBLE);
        disconnectButton.setVisibility(View.INVISIBLE);

    }

    public void GoToAttendance(View view) {

        //register all face back when already add new face
        LoadFaceFromFirebase();
        check = 1;
        tracker.setIdname(userIDFace);


        backButton.setVisibility(View.INVISIBLE);
        fabAdd.setVisibility(View.INVISIBLE);
        bottomSheetLayout.setVisibility(View.VISIBLE);
        connectButton.setVisibility(View.VISIBLE);
        disconnectButton.setVisibility(View.VISIBLE);

    }

    public HashMap<String, String> StoreAttendance(ArrayList<String> Id, ArrayList<String> temperatures) {


        HashMap<String, String> result = new HashMap<>();
        ArrayList<Float> temperature = new ArrayList<>();

        try {
            Float last_temp = 0f;
            if (Id.size() >= 1) {
                result.put("Id", Id.get(Id.size() - 1));


                //to remove unwanted temperature from the list
                for (String tem : temperatures) {
                    if (Float.parseFloat(tem) >= 35) {
                        temperature.add(Float.parseFloat(tem));
                    }
                }

                last_temp = Collections.max(temperature);

                if (temperature.size() == 0) {
                    result.put("Temp", "0.0");
                } else {
//        average=sum/temperature.size();
                    result.put("Temp", stringFourDigits(Float.toString(last_temp)));

//        result.put("Temp",stringFourDigits(Float.parseFloat(Collections.max(temperature,nu[]))));
                }


            }
            if (last_temp <= 37.4) {
                mp.start();
            } else {
                alert.start();
            }

            Log.d("HH", temperatures.toString());


        } catch (Exception e) {
            result.put("Temp", "0");
            result.put("Id", "na");
        }

        return result;

    }


    // alert when the application generate the temperature
    public void InfoDialog() {


        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String hour = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.info_dialog, null);

        TextView name = dialogLayout.findViewById(R.id.finalName);
        TextView temp = dialogLayout.findViewById(R.id.finalTemperature);
        TextView dateText = dialogLayout.findViewById(R.id.finalDate);


        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                check = 1;
                Allow_FaceDetect = true;
                AddedFace = 0;
                IdFace.clear();
                temperatures.clear();

                if (Attendance != 0) {
                sendAttenDanceToFirebase(resultMap.get("Id"), date, userIDFace.get(resultMap.get("Id")), resultMap.get("Temp"));

                    successToast("Checked");
                }

            }


        });
        if (Attendance != 0) {
            name.setText("Name: " + userIDFace.get(resultMap.get("Id")));

        }
        if (resultMap.get("Temp").equals("0.0")) {
            temp.setTextColor(Color.RED);
            temp.setText("Not Properly Checked");
        }
        if (Float.parseFloat(resultMap.get("Temp")) >= 37.5) {
            temp.setTextColor(Color.RED);
            temp.setTextSize(30);
            temp.setText("Temperature: " + stringFourDigits(resultMap.get("Temp")));
            sendMail("Name: " + userIDFace.get(resultMap.get("Id")) + "\n" + "Temperature: " + resultMap.get("Temp") + "\n" + "Location: " + location_txt + "\n" + "Check Time: " + hour);
        } else {
            temp.setTextColor(Color.GREEN);
            temp.setTextSize(30);
            temp.setText("Temperature: " + stringFourDigits(resultMap.get("Temp") + " C"));
        }

        dateText.setText("Date: " + date);
        builder.setView(dialogLayout);
        builder.setCancelable(false);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                    check = 1;
                    Allow_FaceDetect = true;
                    AddedFace = 0;
                    IdFace.clear();
                    temperatures.clear();

                    if (Attendance != 0 && !resultMap.get("Temp").equals("0.0")) {
//                        sendAttenDanceToFirebase(resultMap.get("Id"), date, userIDFace.get(resultMap.get("Id")), resultMap.get("Temp"));
                        try {
                            sendAttendanceToAPI(resultMap.get("email"), userIDFace.get(resultMap.get("email")), resultMap.get("Temp"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        successToast("Checked");
                    }
                    temporary = 0f;
                }
            }
        }, 2000);


    }


    private void TemperatureDialog() {
        check = 0;
        Noattendance = 0;
        ArrayList<String> id = new ArrayList<>();
        id.add("go");
        HashMap<String, String> result = StoreAttendance(id, temperatures);
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.info_dialog, null);
        TextView name = dialogLayout.findViewById(R.id.finalName);
        TextView temp = dialogLayout.findViewById(R.id.finalTemperature);
        TextView dateText = dialogLayout.findViewById(R.id.finalDate);

        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                take = 1;
                check = 1;
                temperatures.clear();
                Noattendance = 0;
                prepare = 0;
            }
        });
        name.setText("Normal");
        dateText.setText(date);

        if (result.get("Temp").equals("0.0")) {
            temp.setTextColor(Color.RED);
            temp.setText("Not Properly Check");
        } else {
            temp.setTextSize(40);
            temp.setText(result.get("Temp"));
        }

        builder.setView(dialogLayout);
        builder.setCancelable(false);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                    take = 1;
                    check = 1;
                    temperatures.clear();
                    Noattendance = 0;
                    prepare = 0;
                    temporary = 0f;

                }
            }
        }, 1500);
    }


//to put the temperature and face into a list

    public void Check() {
        if (tracker.getName() != "" && Allow_FaceDetect) {
            IdFace.add(tracker.getName());
            AddedFace += 1;

        }

        //  Log.d("kkk",String.valueOf(IdFace.size()));
        //  Log.d("kkk",String.valueOf(AddedFace));

        //  we need to have only one face inorder to generate the temperature
        if (AddedFace >= 1 && check == 1 && temperatures.size() > 1) {

            Allow_FaceDetect = false;
            AddedFace = 0;
            ArrayList<String> FaceRepresent = new ArrayList<>(IdFace);
            resultMap = StoreAttendance(FaceRepresent, temperatures);
            IdFace.clear();
            check = 0;

            try {
                InfoDialog();
            } catch (Exception e) {
                check = 1;
                Allow_FaceDetect = true;
                AddedFace = 0;
                IdFace.clear();
                temperatures.clear();
                temporary = 0f;
            }

            temperatures.clear();
        }
    }

    public void getLocationNames() {
        List<Map<String, Object>> locationList = new ArrayList<>();

        //fetch from api
        mQueue = Volley.newRequestQueue(DetectorActivity.this);
        String url = "https://intech-attendance-api-production.up.railway.app/api/v1/location/?format=json";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {

                    for (int i = 0; i < response.length(); i++) {
                        HashMap<String, Object> locate = new HashMap<>();
                        JSONObject jsonObject = response.getJSONObject(i);
                        String name = jsonObject.getString("name");
                        String id = jsonObject.getString("id");
                        locate.put(id, name);

                        locationList.add(locate);
                    }

                    AllLocation = locationList;

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(DetectorActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mQueue.add(request);
    }

    public void sendAttendanceToAPI(String userEmail, String name, String Temperature) throws JSONException {

        location_txt = dropdown.getSelectedItem().toString();
        String Status = "OK";

        Map<String, String> data = new HashMap<>();
        data.put("temperature", Temperature);
        data.put("status", Status);
        data.put("location", location_txt);
        data.put("userEmail", userEmail);

        AttendanceClient attendanceClient = new AttendanceClient(getApplicationContext());
        attendanceClient.sendData(data);
    }


    public void successToast(String Message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.sucess_toast, null);

        TextView text = (TextView) layout.findViewById(R.id.SucessToastMessage);
        text.setText(Message);

        Toast toast = new Toast(getApplicationContext());
        toast.setView(layout);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }


    public void signOut(View view) {
        FirebaseAuth.getInstance().signOut();
        Intent i = new Intent(DetectorActivity.this, login.class);
        startActivity(i);
        finish();

    }


    private void sendMail(String message) {
        for (String email : em) {

            SendMail sm = new SendMail(this, email, "Temperature Warning", message);
            sm.execute();
        }


    }

}
