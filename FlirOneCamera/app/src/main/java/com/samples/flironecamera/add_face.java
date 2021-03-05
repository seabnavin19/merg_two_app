package com.samples.flironecamera;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.samples.flironecamera.tflite.SimilarityClassifier;
import com.samples.flironecamera.tflite.TFLiteObjectDetectionAPIModel;

import java.io.IOException;

public class add_face extends AppCompatActivity {
    private SimilarityClassifier model;
    private static final int SELECT_PICTURE = 1;
    private ImageView newImage;
    private Button addButton;
    private Uri imageuri;
    Bitmap bitmap=null;
    private Button set;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_face);
        newImage=findViewById(R.id.img);
        addButton=findViewById(R.id.add_img);
        set=findViewById(R.id.setImage);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent gal= new Intent();
//                gal.setType("image/*");
//                gal.setAction(Intent.ACTION_GET_CONTENT);
//                startActivityForResult(Intent.createChooser(gal,"select"),SELECT_PICTURE);
                Bitmap icon= BitmapFactory.decodeResource(getResources(),R.drawable.logo);



            }
        });


}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==SELECT_PICTURE && requestCode==RESULT_OK){
            imageuri= data.getData();
            try {
                Bitmap bitmap=MediaStore.Images.Media.getBitmap(getContentResolver(),imageuri);
                newImage.setImageBitmap(bitmap);

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}