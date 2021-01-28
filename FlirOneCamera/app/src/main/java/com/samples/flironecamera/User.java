package com.samples.flironecamera;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.samples.flironecamera.tflite.SimilarityClassifier;

import java.sql.Blob;

public class User {
    String name;
    Bitmap crop =null;
    Integer color=1;
    String id ="";
    float[][] extra= new float[1][];
    float distance=-1;
    RectF location= new RectF();



    public String getName() {
        return name;
    }

    public String getId() { return id; }

    public float getDistance() { return distance; }

    public RectF getLocation() {
        return location;
    }

    public Object getExtra() {
        return extra;
    }
}
