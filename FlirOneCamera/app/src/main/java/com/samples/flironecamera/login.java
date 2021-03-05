package com.samples.flironecamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        FirebaseUser currentuser= auth.getCurrentUser();
        if (currentuser!=null){
            String emailCurrent= currentuser.getEmail();
            Intent i = new Intent(login.this,DetectorActivity.class);
            i.putExtra("Email",emailCurrent);
            startActivity(i);
            finish();
        }
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
            progressDialog.dismiss();
            Alert_Fail();
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
                    progressDialog.dismiss();
                    Alert_Fail();
                    Toast.makeText(login.this,"Fail",Toast.LENGTH_LONG).show();


                }
            });
        }

    }
    private void Alert_Fail(){
        AlertDialog.Builder builder = new AlertDialog.Builder(login.this);
        builder.setTitle("Fail to login!");

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }
    private AlertDialog.Builder builder;

    public void Changepassword(View view){
        Intent i = new Intent(login.this,change_pass.class);
        startActivity(i);
        finish();

        }
}


