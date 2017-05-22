package com.azeesoft.rccarremote.navfragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.azeesoft.rccarremote.R;
import com.azeesoft.rccarremote.tools.bluetooth.RCBluetoothMaster;
import com.azeesoft.rccarremote.tools.wifi.CommConstants;
import com.azeesoft.rccarremote.tools.wifi.IPClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.github.controlwear.virtual.joystick.android.JoystickView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainNavFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainNavFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainNavFragment extends Fragment implements IPClient.OnServerDataReceivedListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TITLE = "title";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mTitle;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private IPClient ipClient;
    private RCBluetoothMaster bluetoothMaster;

    private boolean connectedViaBluetooth = false;

    public MainNavFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param title Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainNavFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainNavFragment newInstance(String title, String param2) {
        MainNavFragment fragment = new MainNavFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_TITLE);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_nav, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().setTitle(mTitle);

        initializeFragment();
    }

    private void initializeFragment(){
        prepareIPClient();
        prepareBluetoothMaster();
        prepareUI();
    }

    private void prepareIPClient(){
        ipClient = IPClient.getIPClient(getActivity());

        if(ipClient.getClientSocket()== null || !ipClient.getClientSocket().isConnected()){
            showWifiErrorOverlay();
        }

        ipClient.setOnServerDataReceivedListener(this);

        ipClient.setOnServerDisconnectedListener(new IPClient.OnServerDisconnectedListener() {
            @Override
            public void onServerDisconnected() {
                showWifiErrorOverlay();
            }
        });
    }

    private void prepareBluetoothMaster(){
        bluetoothMaster = new RCBluetoothMaster();
    }

    private void prepareUI(){
        JoystickView joystick = (JoystickView) getActivity().findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                Log.d("Joystick", "Right analog: Angle=" + angle + " ; Strength=" + strength);

                transmitJoystickData("AnalogV:"+angle+":"+strength+":");
            }
        },100);

        JoystickView joystick2 = (JoystickView) getActivity().findViewById(R.id.joystickView2);
        joystick2.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                Log.d("Joystick", "Left analog: Angle=" + angle + " ; Strength=" + strength);

                transmitJoystickData("AnalogH:"+angle+":"+strength+":");
            }
        },100);

        Button closeConnectionBtn = (Button) getActivity().findViewById(R.id.closeConnectionBtn);
        closeConnectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipClient.disconnect();
            }
        });

        FloatingActionButton connectToRCCarBtn = (FloatingActionButton) getActivity().findViewById(R.id.connectToRCCarBtn);
        connectToRCCarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToRCCar();
            }
        });
    }

    private void showWifiErrorOverlay(){
        final ConstraintLayout constraintLayout = (ConstraintLayout) getActivity().findViewById(R.id.noWifiConnectionOverlay);
        Button connectBtn = (Button) getActivity().findViewById(R.id.connectWifiBtn);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipClient.connect(new IPClient.OnServerConnectedListener() {
                    @Override
                    public void onServerConnectionSucceeded() {
                        constraintLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void onServerConnectionFailed() {
                        Toast.makeText(getActivity(), "Error connecting to your robot Try Again!", Toast.LENGTH_SHORT).show();
                        constraintLayout.setVisibility(View.VISIBLE);
                    }
                },MainNavFragment.this);
            }
        });

        constraintLayout.setVisibility(View.VISIBLE);
    }

    public void closeConnectionWithServer(){
        ipClient.closeConnection();
    }

    private void connectToRCCar(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean connectViaBluetooth = sharedPreferences.getBoolean("control_robot_via_bluetooth", false);

        if(connectViaBluetooth) {
            bluetoothMaster.connectToRCCar();
        }else{
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(CommConstants.RESPONSE_NAME_SUCCESS,true);
                jsonObject.put(CommConstants.RESPONSE_NAME_CONNECT_TO_RC_CAR, true);

                ipClient.sendData(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        connectedViaBluetooth = connectViaBluetooth;
    }

    private void transmitJoystickData(String data){
        if(connectedViaBluetooth){
            bluetoothMaster.sendData(data);
        }else{
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(CommConstants.RESPONSE_NAME_SUCCESS, true);
                jsonObject.put(CommConstants.RESPONSE_NAME_ARDUINO_BLUETOOTH_DATA, data);
                ipClient.sendData(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onServerDataReceived(JSONObject jsonObject) {
        try {
            if(jsonObject.getBoolean(CommConstants.RESPONSE_NAME_SUCCESS)){

            }else{
                if(jsonObject.has(CommConstants.RESPONSE_NAME_FLAGS_ARRAY)){
                    JSONArray flagsArray = jsonObject.getJSONArray(CommConstants.RESPONSE_NAME_FLAGS_ARRAY);
                    for(int i=0; i<flagsArray.length(); i++){
                        CommConstants.RESPONSE_DATA_FLAGS_FAILURE flag = CommConstants.RESPONSE_DATA_FLAGS_FAILURE.valueOf(flagsArray.get(i).toString());
                        switch(flag){
                            case MAX_CONN_REACHED:
                                closeConnectionWithServer();
                                showWifiErrorOverlay();
                                Toast.makeText(getActivity(), "Max Connection limit reached for the robot!", Toast.LENGTH_LONG).show();
                                break;
                            case NON_JSON_DATA:

                                break;
                            default:
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
