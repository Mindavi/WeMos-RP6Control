package com.example.rick.testbuttons;

/**
 * Created by Rick on 3-6-2016.
 */
public final class ConnectionConstants {

    public static final String CONNECTION_BROADCAST_ACTION =
            "com.example.rick.testbuttons.CONNECTION_BROADCAST";

    public static final String EXTENDED_DATA_STATUS =
            "com.example.rick.testbuttons.DATA_STATUS";

    public static final int Connected = 1;
    public static final int NotConnected = 0;

    public static final String MESSAGE_BROADCAST_ACTION =
            "com.example.rick.testbuttons.MESSAGE_BROADCAST";
    public static final String EXTENDED_SEND_MESSAGE_DATA =
            "com.example.rick.testbuttons.SEND_MESSAGE_DATA";
    public static final int Success = 0;
    public static final int NoSocket = 1;
    public static final int PrintWriterError = 2;

}
