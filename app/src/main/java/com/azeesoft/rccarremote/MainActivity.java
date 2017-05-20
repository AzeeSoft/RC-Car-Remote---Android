package com.azeesoft.rccarremote;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothSocket bSocket;
    BufferedReader bReader;
    BufferedWriter bWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                // do whatever you want
                Log.d("Joystick", "Right analog: Angle=" + angle + " ; Strength=" + strength);
                sendData("AnalogV:"+angle+":"+strength+":");
            }
        },200);

        JoystickView joystick2 = (JoystickView) findViewById(R.id.joystickView2);
        joystick2.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                // do whatever you want
                Log.d("Joystick", "Left analog: Angle=" + angle + " ; Strength=" + strength);
                sendData("AnalogH:"+angle+":"+strength+":");
            }
        },200);

    }

    @Override
    protected void onResume() {
        super.onResume();

//        connectToDevice();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1010: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    connectToDevice();
                } else {
                    Toast.makeText(this,"Permission Denied!",Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void connectToDevice(View view){
        connectToDevice();
    }

    void connectToDevice(){
        closeAllConnections();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        for (BluetoothDevice bDev : bluetoothAdapter.getBondedDevices()) {
            if (bDev.getName().equals("HC-06")) {
                try {
                    bSocket = bDev.createInsecureRfcommSocketToServiceRecord(bDev.getUuids()[0].getUuid());
                    bSocket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("",e.getMessage());
                    try {
                        Log.e("","trying fallback...");

                        bSocket =(BluetoothSocket) bDev.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(bDev,1);
                        bSocket.connect();

                        Log.e("","Connected");
                    }
                    catch (Exception e2) {
                        Log.e("", "Couldn't establish Bluetooth connection!");
                    }
                }
            }
        }

        if(bSocket!=null){
            try {
                OutputStream outputStream = bSocket.getOutputStream();
                InputStream inStream = bSocket.getInputStream();

                bReader = new BufferedReader(new InputStreamReader(inStream));
                bWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                sendData("AZEE_HANDSHAKE");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void sendData(String msg){
        try {
            if(bWriter!=null) {
                bWriter.write(msg+"\n");
                bWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        closeAllConnections();
    }

    void closeAllConnections(){
        try {
            if(bReader!=null)
                bReader.close();
            if(bWriter!=null)
                bWriter.close();
            if(bSocket!=null)
                bSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
