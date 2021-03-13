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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Gravity;
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

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.samples.flironecamera.customview.OverlayView;
import com.samples.flironecamera.customview.OverlayView.DrawCallback;
import com.samples.flironecamera.env.BorderedText;
import com.samples.flironecamera.env.ImageUtils;
import com.samples.flironecamera.env.Logger;
import com.samples.flironecamera.tflite.SimilarityClassifier;
import com.samples.flironecamera.tflite.TFLiteObjectDetectionAPIModel;
import com.samples.flironecamera.tracking.MultiBoxTracker;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();


  // FaceNet
  private static final int TF_OD_API_INPUT_SIZE = 160;
  private static final boolean TF_OD_API_IS_QUANTIZED = false;
  private static final String TF_OD_API_MODEL_FILE = "facenet.tflite";
  //private static final String TF_OD_API_MODEL_FILE = "facenet_hiroki.tflite";

  // MobileFaceNet
//  private static final int TF_OD_API_INPUT_SIZE = 112;
//  private static final boolean TF_OD_API_IS_QUANTIZED = false;
//  private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";


  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;

  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  //private static final int CROP_SIZE = 320;
  //private static final Size CROP_SIZE = new Size(320, 320);


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
  //private Matrix cropToPortraitTransform;

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
  private Boolean Allow_FaceDetect=true;
  private int AddedFace=0;
  private  HashMap<String,String> resultMap;
  private  int Noface;
  private String ID;
  private String currentuser;
  public  int Attendance;
  private MediaPlayer mp;
  private MediaPlayer alert;


  //to hide this 2 button
  private Button attendance;
  private Button add_people;



  //per location
  private  EditText location;
  private String location_txt="three";
  private FirebaseFirestore db;

  //private HashMap<String, Classifier.Recognition> knownFaces = new HashMap<>();

  public String stringFourDigits(String str) {
    return str.length() < 4 ? str : str.substring(0, 4);
  }

  public  String stringTwoDigits(String str) {
    return str.length() < 2 ? str : str.substring(0, 2);
  }


  //list of face fetch from database
  private List<Map<String,Object>> AllFaceFromDataBase;
  private Button addButton;
  private Spinner dropdown;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent i = getIntent();
    String email= i.getStringExtra("Email");
    if (email.equals("No")){
      Attendance=0;
    }
    else {

      dropdown = findViewById(R.id.spinner1);

      String[] items = new String[]{"1", "2", "three"};

      ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);

      dropdown.setAdapter(adapter);

      AskingDialog();
      currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid();


      dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
          location_txt= dropdown.getSelectedItem().toString();
          successToast("mother");
          RegisterFaceFromFireBase();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
      });

      Attendance=1;
      Log.d("user",currentuser);
    }
    mp=MediaPlayer.create(this,R.raw.temprature_checked);
    alert=MediaPlayer.create(this,R.raw.alert);
    fabAdd = findViewById(R.id.fab_add);
    connectButton=findViewById(R.id.connect_to_flir);
    disconnectButton=findViewById(R.id.disconnect_flir);
    backButton=findViewById(R.id.ChangeAttendance);
    attendance=findViewById(R.id.text_attendance);
    add_people=findViewById(R.id.GotoAddFace);

    //
//    location= findViewById(R.id.location);



    if (Attendance==0){
      add_people.setVisibility(View.INVISIBLE);
      attendance.setVisibility(View.INVISIBLE);
    }
    IdFace= new HashSet<>();
    temperatures= new ArrayList<>();
    fabAdd.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onAddClick();
      }
    });

    // Real-time contour detection of multiple faces
    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();

    FaceDetector detector = FaceDetection.getClient(options);

    faceDetector = detector;



    //checkWritePermission();





  }



  private void onAddClick() {

    addPending = true;
    //Toast.makeText(this, "click", Toast.LENGTH_LONG ).show();

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
    }
    else {
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

//    frameToCropTransform =
//            ImageUtils.getTransformationMatrix(
//                    previewWidth, previewHeight,
//                    previewWidth, previewHeight,
//                    sensorOrientation, MAINTAIN_ASPECT);

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

  int k=0;
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

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
    faceDetector
            .process(image)
            .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
              @Override
              public void onSuccess(List<Face> faces) {
                if (faces.size() == 0) {
                  updateResults(currTimestamp, new LinkedList<>());
                  temperatureText.setText("");
                  Noface+=1;
                  IdFace.clear();
                  temperatures.clear();
                  temperatureData=null;
                  return;

                }
                else {
                  temperatureText.setText(temperatureData+" °C");

                }
                runInBackground(
                        new Runnable() {
                          @Override
                          public void run() {

                              onFacesDetected(currTimestamp, faces, addPending);
                              addPending = false;

                          }
                        });
              }

            });


  }

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

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.
//        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//        final int inWidth = transpose ? srcHeight : srcWidth;
//        final int inHeight = transpose ? srcWidth : srcHeight;

    if (applyRotation != 0) {

      // Translate back from origin centered reference to destination frame.
      matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
    }

    return matrix;

  }


  public SimilarityClassifier.Recognition NewPerson;

  private void showAddFaceDialog(SimilarityClassifier.Recognition rec) {

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = getLayoutInflater();
    View dialogLayout = inflater.inflate(R.layout.image_edit_dialog, null);
    ImageView ivFace = dialogLayout.findViewById(R.id.dlg_image);
    TextView tvTitle = dialogLayout.findViewById(R.id.dlg_title);
    EditText etName = dialogLayout.findViewById(R.id.dlg_input);
    EditText id= dialogLayout.findViewById(R.id.id_input);

    tvTitle.setText("Add Face");
    rec.setColor(1);
    ivFace.setImageBitmap(rec.getCrop());
    NewPerson=rec;
    etName.setHint("Enter name");
    id.setHint("Enter Id");
    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

      }
    });

    builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
      @Override
      public void onClick(DialogInterface dlg, int i) {
          String name = etName.getText().toString();
          detector.register(name,rec);
          NameFromFirebase=name;
          ID=id.getText().toString();
          if (name.isEmpty()) {
              return;
          }
          builder.setCancelable(false);
          progressDialog= new ProgressDialog(DetectorActivity.this);
          progressDialog.setMessage("Uploading Image To DataBase..!");
          progressDialog.show();
          AddNewFace();
          dlg.dismiss();
      }
    });
    builder.setView(dialogLayout);
    builder.setCancelable(false);
    builder.show();


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
            new Runnable() {
              @Override
              public void run() {
                showFrameInfo(previewWidth + "x" + previewHeight);
                showCropInfo(croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());
                showInference(lastProcessingTimeMs + "ms");
              }
            });

  }
  int take=1;

  private void onFacesDetected(long currTimestamp, List<Face> faces, boolean add) {

    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
    final Canvas canvas = new Canvas(cropCopyBitmap);
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
    boolean saved = false;
    Face facea=faces.get(0);
    final RectF boundingBoxt = new RectF(facea.getBoundingBox());
    if (Attendance==0){
      if ((boundingBoxt.left-boundingBoxt.right)<-150.0 && (take==1)){
        if (temperatureData==null){
          temperatureData="0";
        }
        Float temp=Float.parseFloat(temperatureData);
        temperatures.add(temperatureData);
      }
      if (temperatures.size()>=10){
        take=0;
        TemperatureDialog();
      }
    }


    for (Face face : faces) {

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
//          Object extra = result.getExtra();
//          if (extra != null) {
//            LOGGER.i("embeeding retrieved " + extra.toString());
//          }

          float conf = result.getDistance();
          if (conf < 0.65f) {
            confidence = conf;
            label = result.getTitle();
            if (result.getId().equals("0")) {
              color = Color.GREEN;
//              Float temp= Float.parseFloat(temperatureData);
              if (temperatureData==null){
                temperatureData="0";
              }
//              Float temp=Float.parseFloat(temperatureData);
              temperatures.add(temperatureData);

              Check(result.getCrop());

              Log.d("MyFace",String.valueOf(AddedFace));

            }
            else {
              color = Color.RED;
            }
          }

        }

        if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {

          // camera is frontal so the image is flipped horizontally
          // flips horizontally
          Matrix flip = new Matrix();
          if (sensorOrientation == 90 || sensorOrientation == 270) {
            flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
          }
          else {
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



    //    if (saved) {
//      lastSaved = System.currentTimeMillis();
//    }


    updateResults(currTimestamp, mappedRecognitions);


  }
  private ImageView testImage;

//  public static byte[] getBytes(Bitmap bitmap) {
//    ByteArrayOutputStream stream = new ByteArrayOutputStream();
//    bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
//    return stream.toByteArray();
//  }
//
//
//  public Bitmap getBitmap(byte[] bytedata){
//    Bitmap bitmap = BitmapFactory.decodeByteArray(bytedata, 0, bytedata.length);
//    return bitmap;
//  }
  public void addFaceToFireBase (View view){

    AddNewFace();
  }

  //Dialog for user to decide to have attendance checking or not
  public void AskingDialog(){
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = getLayoutInflater();
    View dialogLayout = inflater.inflate(R.layout.askingoption, null);
    TextView asktitle= dialogLayout.findViewById(R.id.ask_title);

    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        LoadFaceFromFirebase();
      }
    });
//    builder.setCanceledOnTouchOutside(false);
    builder.setCancelable(false);


    builder.setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

      }
    });
    builder.setView(dialogLayout);
    builder.show();
  }



  public void FetchAllFaceFromFirebase(View view){
    LoadFaceFromFirebase();
//    AddNewFace();
  }

  private HashMap<String,String> userIDFace= new HashMap<>();
  public void LoadFaceFromFirebase(){
    for (Map<String, Object> document : AllFaceFromDataBase){
      float[][] Extra= new float[1][];
//      Toast.makeText(DetectorActivity.this,String.valueOf(AllFaceFromDataBase),Toast.LENGTH_LONG).show();
      float Distance= Float.parseFloat(String.valueOf(document.get("Distance")));
      ArrayList<Float> ArrayListExtra= (ArrayList<Float>) document.get("Extra");
      float[] arr0= new float[ArrayListExtra.size()];
      int i=0;
      String Name=document.get("Name").toString();
      userIDFace.put(Name,document.get("ID").toString());

      for (int k=0;k<ArrayListExtra.size();k++){
        arr0[k]= Float.parseFloat(String.valueOf(ArrayListExtra.get(k)));
      }
      Extra[0]=arr0;
      SimilarityClassifier.Recognition Newface= new SimilarityClassifier.Recognition(document.get("Id").toString(),document.get("Title").toString(),Distance,new RectF());
      Newface.setExtra(Extra);
      detector.register(Name,Newface);


    }
    Toast.makeText(DetectorActivity.this,"Sucess register",Toast.LENGTH_LONG).show();
  }

  public void AddNewFace(){
    FirebaseFirestore db= FirebaseFirestore.getInstance();
    Map<String, Object> user = new HashMap<>();
    ArrayList<Float> n= new ArrayList<>();
    user.put("title",NewPerson.getTitle().toString());
    user.put("Id_image",NewPerson.getId());
    user.put("distance",NewPerson.getDistance());;
    user.put("Name",NameFromFirebase);
    user.put("ID",ID);
//    user.put("crop", Blob.fromBytes(getBytes(navin.getCrop())));
    int p=0;
    for (float[] i:NewPerson.getExtra()){
      for (float j : i){
        n.add(j);
      }
      user.put("n"+ p,n);
      p+=1;
    }
    location_txt= dropdown.getSelectedItem().toString();
    People_Face people_face = new People_Face(NewPerson.getId(),NameFromFirebase,NewPerson.getDistance(),n,NewPerson.getTitle());
    db.collection(currentuser).document("Face").update(location_txt+"."+ID,people_face).addOnSuccessListener(new OnSuccessListener<Void>() {
      @Override
      public void onSuccess(Void aVoid) {
        progressDialog.dismiss();
      }
    });

  }


public void RegisterFaceFromFireBase() {
  FirebaseFirestore db = FirebaseFirestore.getInstance();
  db.collection(currentuser).document("Face").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
    @Override
    public void onSuccess(DocumentSnapshot documentSnapshot) {
      if (documentSnapshot.get(location_txt)!=null){
        HashMap<String,Object> me= (HashMap<String, Object>) documentSnapshot.get(location_txt);
        List<Map<String, Object>> usersReplace = new ArrayList<>();
        Log.d("kkkkk",me.toString());
        for (Map.Entry element: me.entrySet()){
           Map<String, Object> user = new HashMap<>();
           String key = (String) element.getKey();
           HashMap<String,Object> by_key= (HashMap<String, Object>) me.get(key);
           user.put("Id",by_key.get("id"));
           user.put("Distance",by_key.get("distance"));
           user.put("Title",by_key.get("title"));
           user.put("Extra",by_key.get("face"));
           user.put("Name",by_key.get("name"));
           user.put("ID",key);
           usersReplace.add(user);
//           Log.d("kkkkk",user.toString());

         }
        AllFaceFromDataBase=usersReplace;
        successToast(String.valueOf(usersReplace.size()));

      }
      else {
        AllFaceFromDataBase = new ArrayList<>();
        Log.d("kkkkk",location_txt);

      }
      detector.unregister();


    }
  });


}

//private ImageView im;
public void GotoAddFace(View view){
    fabAdd.setVisibility(View.VISIBLE);
    backButton.setVisibility(View.VISIBLE);
    bottomSheetLayout.setVisibility(View.INVISIBLE);
    detector.unregister();
    connectButton.setVisibility(View.INVISIBLE);
    disconnectButton.setVisibility(View.INVISIBLE);
//  im=findViewById(R.id.newFaceImage);
//  BitmapDrawable drawable= (BitmapDrawable) im.getDrawable();
//  Bitmap bitmap_im= drawable.getBitmap();
//  InputImage image= InputImage.fromBitmap(bitmap_im,0);
//  faceDetector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
//    @Override
//    public void onSuccess(List<Face> faces) {
//      if (faces.size()==0)
//    }
//  });







}

public void GoToAttendance(View view){
    LoadFaceFromFirebase();
    backButton.setVisibility(View.INVISIBLE);
    fabAdd.setVisibility(View.INVISIBLE);
    bottomSheetLayout.setVisibility(View.VISIBLE);
    connectButton.setVisibility(View.VISIBLE);
    disconnectButton.setVisibility(View.VISIBLE);

}

public HashMap<String,String> StoreAttendance(ArrayList<String> Id, ArrayList<String> temperatures){
    HashMap<String,String> result= new HashMap<>();
    ArrayList<String> temperature= new ArrayList<>();



    if (Id.size()==1){
      result.put("Id",Id.get(0));
      Float sum=0f;
      Float average;

      //to remove unwanted temperature from the list
      for (String tem: temperatures){
        if (Float.parseFloat(tem)>35){
//          temperatures.remove(tem);
          temperature.add(tem);
          Toast.makeText(DetectorActivity.this,"hello",Toast.LENGTH_LONG).show();
        }
      }


      // to find the average temperature

      for (String i: temperature){
        if (i==null){
          sum+=0;
        }else {
          sum+=Float.parseFloat(i);
        }
      }
      if (temperature.size()==0){
        result.put("Temp","0.0");
      }
      else {
        average=sum/temperature.size();
        result.put("Temp",stringFourDigits(Float.toString(average)));
      }

    }

    Log.d("HH",temperatures.toString());
    mp.start();

    return result;

}



// alert when the application generate the temperature
public void InfoDialog(){

  String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

  AlertDialog.Builder builder = new AlertDialog.Builder(this);
  LayoutInflater inflater = getLayoutInflater();
  View dialogLayout = inflater.inflate(R.layout.info_dialog, null);
  TextView name= dialogLayout.findViewById(R.id.finalName);
  TextView temp=dialogLayout.findViewById(R.id.finalTemperature);
  TextView dateText=dialogLayout.findViewById(R.id.finalDate);


  builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialog, int which) {
      Allow_FaceDetect=true;
      AddedFace=0;
      IdFace.clear();
      temperatures.clear();
      if (Attendance!=0){
          sendAttenDanceToFirebase(userIDFace.get(resultMap.get("Id")),date,resultMap.get("Id"),resultMap.get("Temp"));
      }

    }
  });
  if (Attendance!=0){
      name.setText("Name: "+resultMap.get("Id"));
  }
  temp.setText("Temperature: "+stringFourDigits(resultMap.get("Temp")));
  dateText.setText("Date: "+date);
  builder.setView(dialogLayout);
  builder.setCancelable(false);
  builder.show();



}



 private  void TemperatureDialog() {
    ArrayList<String> id= new ArrayList<>();
    id.add("go");
    HashMap<String,String> result = StoreAttendance(id,temperatures);
     temperatures.clear();
     String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
     AlertDialog.Builder builder = new AlertDialog.Builder(this);
     LayoutInflater inflater = getLayoutInflater();
     View dialogLayout = inflater.inflate(R.layout.info_dialog, null);
     TextView name= dialogLayout.findViewById(R.id.finalName);
     TextView temp=dialogLayout.findViewById(R.id.finalTemperature);
     TextView dateText=dialogLayout.findViewById(R.id.finalDate);
     builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
             take=1;
             temperatures.clear();
             alert.stop();
             alert=MediaPlayer.create(DetectorActivity.this,R.raw.alert);
         }
     });
     name.setText("Normal");
     dateText.setText(date);
     temp.setText(result.get("Temp"));
     builder.setView(dialogLayout);
     builder.show();
     if (Float.parseFloat(result.get("Temp"))==0){
         alert.start();

     }
//     temp.setText(stringFourDigits(String.valueOf(avg)));
}


//to put the temperature and face into a list

public void Check(Bitmap bitmap){
  if (tracker.getName()!="" && Allow_FaceDetect){
    IdFace.add(tracker.getName());
    AddedFace+=1;

  }

  // we need to have only one face inorder to generate the temperature
  if (AddedFace==9 && IdFace.size()==1){
    Allow_FaceDetect=false;
    AddedFace=0;
    ArrayList<String> FaceRepresent= new ArrayList<>(IdFace);
//                FaceRepresent.addAll(IdFace);
    resultMap=StoreAttendance(FaceRepresent,temperatures);
    temperatures.clear();
    IdFace.clear();
    InfoDialog();
  }
  if (AddedFace==5 && IdFace.size()>1){
    IdFace.clear();
    temperatures.clear();
    AddedFace=0;
  }
}


public void sendAttenDanceToFirebase(String ID,String Date,String Name,String Temperature){
  db = FirebaseFirestore.getInstance();
    location_txt= dropdown.getSelectedItem().toString();
    String Status="OK";
    String hour = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
  Present_student student = new Present_student(ID,Name,Status,Temperature,Date,location_txt,hour);
  Map<String, Object> user1 = new HashMap<>();
  Map<String, Object> user3 = new HashMap<>();
  ArrayList<Object> user2= new ArrayList<>();
    db.collection(currentuser).document("Attendance").update(location_txt+"."+Date,FieldValue.arrayUnion(student));
}


public void successToast(String Message){
  LayoutInflater inflater = getLayoutInflater();
  View layout = inflater.inflate(R.layout.sucess_toast,null);

  TextView text = (TextView) layout.findViewById(R.id.SucessToastMessage);
  text.setText(Message);

  Toast toast= new Toast(getApplicationContext());
  toast.setView(layout);
  toast.setDuration(Toast.LENGTH_LONG);
  toast.show();
}


public void signOut(View view){
    FirebaseAuth.getInstance().signOut();
    Intent i = new Intent(DetectorActivity.this,login.class);
    startActivity(i);
    finish();
}


// to modified the location
  public void InputLocation(View view){
    String locate=location.getText().toString();
    location_txt= locate;
    successToast("Change location to "+ location_txt);

  }



}
