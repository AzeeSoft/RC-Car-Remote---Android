package com.azeesoft.rccarremote.tools.wifi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by azizt on 5/20/2017.
 */

public class IPClient {
    final String LOG_TAG = "IP CLIENT";

    public final static int DEFAULT_PORT = 6060;

    private String serverAddress = "";
    private int serverPort = DEFAULT_PORT;

    private Socket clientSocket;
    private BufferedWriter bufferedWriter;

    private Handler updateOnUIHandler;
    private OnServerConnectedListener onServerConnectedListener;
    private OnServerDataReceivedListener onServerDataReceivedListener;
    private OnServerDisconnectedListener onServerDisconnectedListener;

    private static IPClient thisClient;

    public static IPClient getIPClient(Context context) {
        if (thisClient == null) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String ipAddress = sharedPreferences.getString("server_ip_address", "");
            String port = sharedPreferences.getString("server_port", "6060");

            thisClient = new IPClient(ipAddress, Integer.parseInt(port));
        }

        return thisClient;
    }

    private IPClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        serverPort = port;
        updateOnUIHandler = new Handler();
    }

    public void connect(OnServerConnectedListener onServerConnectedListener, OnServerDataReceivedListener onServerDataReceivedListener) {
        setOnServerConnectedListener(onServerConnectedListener);
        setOnServerDataReceivedListener(onServerDataReceivedListener);
        initiateConnection();
    }

    private void initiateConnection() {
        new Thread(new ServerConnectRunnable()).start();
    }

    public void closeConnection() {
        try {
            if (bufferedWriter != null)
                bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (clientSocket.isConnected())
                clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendData(JSONObject data) {
        if (bufferedWriter != null && clientSocket != null && clientSocket.isConnected()) {
            try {
                bufferedWriter.write(data + "\n");
                bufferedWriter.flush();
                Log.d(LOG_TAG, "Data written");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void disconnect() {
        closeConnection();
        if (onServerDisconnectedListener != null) {
            onServerDisconnectedListener.onServerDisconnected();
        }
    }

    public void setOnServerDataReceivedListener(OnServerDataReceivedListener onServerDataReceivedListener) {
        this.onServerDataReceivedListener = onServerDataReceivedListener;
    }

    public void setOnServerConnectedListener(OnServerConnectedListener onServerConnectedListener) {
        this.onServerConnectedListener = onServerConnectedListener;
    }

    public void setOnServerDisconnectedListener(OnServerDisconnectedListener onServerDisconnectedListener) {
        this.onServerDisconnectedListener = onServerDisconnectedListener;
    }

    private class ServerConnectRunnable implements Runnable {

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(serverAddress);
                clientSocket = new Socket(serverAddr, serverPort);
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                new Thread(new ServerListenerRunnable(new BufferedReader(new InputStreamReader(clientSocket.getInputStream())))).start();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(CommConstants.RESPONSE_NAME_SUCCESS, true);

                JSONArray flags = new JSONArray();
                flags.put(CommConstants.RESPONSE_DATA_FLAGS_SUCCESS.AZEE_IP_HANDSHAKE);

                jsonObject.put(CommConstants.RESPONSE_NAME_MESSAGE, "Initializing Connection");

                sendData(jsonObject);

                updateOnUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onServerConnectedListener != null) {
                            onServerConnectedListener.onServerConnectionSucceeded();
                        }
                    }
                });
            } catch (IOException | JSONException e) {
                e.printStackTrace();

                updateOnUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onServerConnectedListener != null) {
                            onServerConnectedListener.onServerConnectionFailed();
                        }
                    }
                });
            }
        }
    }

    private class ServerListenerRunnable implements Runnable {

        private BufferedReader bufferedReader;

        ServerListenerRunnable(BufferedReader bReader) {
            bufferedReader = bReader;
        }

        @Override
        public void run() {

            try {
                while (!Thread.currentThread().isInterrupted()) {

                    if (bufferedReader != null) {
                        String incomingData = bufferedReader.readLine();
                        if(incomingData==null){
                            break;
                        }

                        if (!incomingData.isEmpty()) {
                            Log.d(LOG_TAG, "Incoming data from server: " + incomingData);

                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject(incomingData);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                jsonObject = new JSONObject();
                                try {
                                    jsonObject.put(CommConstants.RESPONSE_NAME_SUCCESS, false);

                                    JSONArray flags = new JSONArray();
                                    flags.put(CommConstants.RESPONSE_DATA_FLAGS_FAILURE.NON_JSON_DATA);

                                    jsonObject.put(CommConstants.RESPONSE_NAME_FLAGS_ARRAY, flags);
                                    jsonObject.put(CommConstants.RESPONSE_NAME_NON_JSON_DATA, incomingData);
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }
                            }

                            updateOnUIHandler.post(new UpdateUIRunnable(jsonObject));
                        }
                    }else{
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            updateOnUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            });
        }
    }

    private class UpdateUIRunnable implements Runnable {

        JSONObject jsonObject;

        UpdateUIRunnable(JSONObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            if (onServerDataReceivedListener != null) {
                onServerDataReceivedListener.onServerDataReceived(jsonObject);
            }
        }
    }

    public interface OnServerConnectedListener {
        void onServerConnectionSucceeded();

        void onServerConnectionFailed();
    }

    public interface OnServerDataReceivedListener {
        void onServerDataReceived(JSONObject jsonObject);
    }

    public interface OnServerDisconnectedListener {
        void onServerDisconnected();
    }
}