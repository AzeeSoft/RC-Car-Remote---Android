package com.azeesoft.rccarremote.tools.wifi;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by azizt on 5/28/2017.
 */
public class MicReceiver {
    final String TAG = "MicReceiver";

    static boolean isReceiving = true;
    static int port = 50005;
    static int sampleRate = 44100;

    DatagramSocket serverSocket;

    private static MicReceiver thisReceiver;

    public static MicReceiver getMicReceiver(){
        if(thisReceiver==null){
            thisReceiver = new MicReceiver();
        }

        return thisReceiver;
    }

    private MicReceiver(){

    }

    public void startReceiving(){

        if(isReceiving){
            stopReceiving();
        }

        Thread receiveAudioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                AudioTrack mAudioTrack=null;
                try {
                    serverSocket = new DatagramSocket(port);

//                    int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    int bufferSize = 3584;

                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

                    mAudioTrack.play();

                    Log.d(TAG, "bufferSize: "+bufferSize);

                    byte[] receiveData = new byte[bufferSize];

                    DatagramPacket receivePacket = new DatagramPacket(receiveData,
                            receiveData.length);
                    while (isReceiving) {
                        serverSocket.receive(receivePacket);
                        mAudioTrack.write(receiveData, 0, receiveData.length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(mAudioTrack!=null){
                    mAudioTrack.stop();
                }
            }
        });

        isReceiving=true;
        receiveAudioThread.start();
    }

    public void stopReceiving(){
        isReceiving=false;
        if(serverSocket!=null){
            try {
                serverSocket.disconnect();
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
