package com.samples.flironecamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class login extends AppCompatActivity {
    private FirebaseAuth auth;
    private Button login_button;
    private EditText email;
    private EditText password;
    private Button without_acc;
    ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth=FirebaseAuth.getInstance();
        email=findViewById(R.id.email);
        password=findViewById(R.id.pass);
        progressDialog= new ProgressDialog(login.this);
        login_button=findViewById(R.id.login_but);
        without_acc=findViewById(R.id.no_acc);
        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt_email= email.getText().toString();
                String txt_pass=password.getText().toString();
                progressDialog.setMessage("Login....");
                progressDialog.show();
                loginUser(txt_email,txt_pass);
            }
        });
        without_acc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(login.this,DetectorActivity.class);
                i.putExtra("Email","No");
                startActivity(i);
                finish();
            }
        });
    }
    private void loginUser(String email,String pass){
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)){
            Toast.makeText(login.this,"Fail",Toast.LENGTH_LONG).show();
        }
        else {
            auth.signInWithEmailAndPassword(email,pass).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    Toast.makeText(login.this,"Success",Toast.LENGTH_LONG).show();
                    Intent i = new Intent(login.this,DetectorActivity.class);
                    i.putExtra("Email",email);
                    startActivity(i);
                    finish();
                    progressDialog.dismiss();

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(login.this,"Fail",Toast.LENGTH_LONG).show();

                }
            });
        }

    }
}