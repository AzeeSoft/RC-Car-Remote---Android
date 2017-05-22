package com.azeesoft.rccarremote.tools.wifi;

/**
 * Created by azizt on 5/20/2017.
 */

public class CommConstants {

    /*
    * Common data sent from both Server and Client
    * */

    public enum RESPONSE_DATA_FLAGS_SUCCESS {AZEE_IP_HANDSHAKE}
    public enum RESPONSE_DATA_FLAGS_FAILURE {MAX_CONN_REACHED, NON_JSON_DATA}

    public final static String RESPONSE_NAME_SUCCESS = "success";
    public final static String RESPONSE_NAME_FLAGS_ARRAY = "flags_array";
    public final static String RESPONSE_NAME_NON_JSON_DATA = "non_json_data";
    public final static String RESPONSE_NAME_MESSAGE = "message";


    /*
    * Data sent from Server
    * */



    /*
    * Data sent from Client
    * */

    public final static String RESPONSE_NAME_ARDUINO_BLUETOOTH_DATA = "arduino_bluetooth_data";
    public final static String RESPONSE_NAME_CONNECT_TO_RC_CAR = "connect_to_rc_car";

}
