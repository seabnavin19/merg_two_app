package com.samples.flironecamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;

public class test_home extends AppCompatActivity{
    private Button temp;
    public String temperatureData="000";
    private TextView temperatureText;
    public PermissionHandler permissionHandler;
    int k=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        goToNextAction=1;
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_test_home);
//        temp=findViewById(R.id.temperature_test);
        temperatureText=findViewById(R.id.temperatureText);
        temperatureText.setTextSize(50);
//        temperatureText=findViewById(R.id.temperatureText_test);
        permissionHandler = new PermissionHandler(showMessage, test_home.this);
        startService(new Intent(test_home.this,MyService.class));
//        temp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                Message msg = Message.obtain(null, MyService.MSG_SAY_HELLO, 0, 0);
////                try {
////                    mService.send(msg);
////                    Log.v(TAG, "Message sent.");
////                } catch ( RemoteException e) {
////                    e.printStackTrace();
////                }
//
//                Intent i = new Intent(test_home.this,login.class);
//                startActivity(i);
//                stop();
//                finish();
//
//
////                Intent u = new Intent(test_home.this,MainActivity.class);
//////        u.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
////        startActivity(u);
////                stopService(new Intent(test_home.this,MyService.class));
//            }
//        });
//        why();
    }

    private MainActivity.ShowMessage showMessage = message -> Toast.makeText(test_home.this, message, Toast.LENGTH_SHORT).show();
    public void stop(){
        stopService(new Intent(test_home.this,MyService.class));
        if (mBound) {
            test_home.this.unbindService(mConnection);
            mBound = false;
        }
    }


    Messenger mService;
    boolean mBound = false;
    final String TAG = "MainFragment";

    public test_home() {
        // Required empty public constructor
    }

    MyReceiver myReceiver;
    @Override
    public void onStart() {
//        myReceiver = new test_home.MyReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(MyService2.MY_ACTION);
//        registerReceiver(myReceiver, intentFilter);
        myReceiver = new test_home.MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyService.MY_ACTION);
        registerReceiver(myReceiver, intentFilter);

        //Start our own service
//        Intent i = new Intent(test_home.this,
//                MyService.class);
//        startService(i);
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(test_home.this, MyService.class);
        test_home.this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
//        unregisterReceiver(myReceiver);
        super.onStop();
        // Unbind from the service
        unregisterReceiver(myReceiver);
        if (mBound) {
            test_home.this.unbindService(mConnection);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(test_home.this,MyService.class));


    }


    //why
    private void why(){
        handler.postDelayed( runnable = new Runnable() {
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
    }

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 3*1000; //Delay for 15 seconds.  One second = 1000 milliseconds.

//    CameraHandler cameraHandler= MyService.cameraHandler;
    @Override
    protected void onResume() {
        //start handler as activity become visible
//        startService(new Intent(test_home.this,MyService.class));

        why();

        super.onResume();
    }

// If onPause() is not included the threads will double up when you
// reload the activity

    private class MyReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            String datapassed = intent.getStringExtra("DATAPASSED");
            temperatureData=datapassed;
            temperatureText.setText(datapassed);
            if (Float.parseFloat(datapassed)>29 && k==0){
                k=1;
                Intent i = new Intent(test_home.this,DetectorActivity.class);
                startActivity(i);
                stop();
                finish();
            }




        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Message msg = Message.obtain(null, MyService.MSG_SAY_HELLO, 0, 0);
        try {
                    mService.send(msg);

//                Toast.makeText(test_home.this,MyService.cameraHandler.getInfo(),Toast.LENGTH_SHORT).show();

            Log.v(TAG, "Message sent.");
        } catch ( Exception e) {
            e.printStackTrace();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }

}