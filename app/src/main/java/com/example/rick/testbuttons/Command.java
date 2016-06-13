package com.example.rick.testbuttons;

import java.util.Enumeration;

/**
 * Created by Rick on 6-6-2016.
 */
public enum Command implements ICommand {
    MAXSPEED,
    ANGLE,
    SPEED,
    MAX_LENGTH_ERROR,
    CONNECTION_ERROR,
    INVALID_COMMAND_ERROR,
    CONTROL;
    public enum DIRECTION {
        LEFT,
        RIGHT,
        FORWARD,
        BACKWARD,
        NONE;
    }

    public static String CommandStringBuilder(DIRECTION direction) {
        return String.format("%s:%s", DIRECTION.class.getSimpleName(), direction);
    }

    public static String CommandStringBuilder(ICommand command, String arg) {
        return String.format("%s:%s", command, arg);
    }

    public static String CommandStringBuilder(ICommand command, int arg) {
        return String.format("%s:%d", command, arg);
    }

}
