package com.samples.flironecamera;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;


public class change_pass extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText email_changed;
    private Button send_but;
    private TextView text_de;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pass);
        auth=FirebaseAuth.getInstance();
        email_changed=findViewById(R.id.email_changed);
        send_but=findViewById(R.id.change);
        text_de=findViewById(R.id.descript);
    }

    public void Changepass(View view){
        String email_txt= email_changed.getText().toString();
        auth.sendPasswordResetEmail(email_txt).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                email_changed.setVisibility(View.INVISIBLE);
                send_but.setVisibility(View.INVISIBLE);
                text_de.setText("Please Check your Gmail");
            }
        });

    }
}