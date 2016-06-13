package com.example.rick.testbuttons;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import junit.framework.Assert;

public class ControlRobotActivity extends AppCompatActivity {
    private final String TAG = "ControlRobotActivity";
    private TextView tvStatus;
    private TextView tvMessage;
    private int messageCounter;
    private int maxSpeed;
    private final int MAXIMUM_SPEED_ALLOWED = 130;
    private int oldX = 0;
    private int oldY = 0;
    private int oldSpeed = 0;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ConnectionManager.getInstance().disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ConnectionManager.getInstance().disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlrobot);

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.ControlRobotActivity);
        if (relativeLayout != null) {
            relativeLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int deltaX = Math.round(Math.abs(oldX - event.getX()));
                    int deltaY = Math.round(Math.abs(oldY - event.getY()));
                    if (deltaX > 50 || deltaY > 50) {
                        Command.DIRECTION yDirection = Command.DIRECTION.NONE; // init on none
                        Command.DIRECTION xDirection = Command.DIRECTION.NONE;
                        oldX = Math.round(event.getX());
                        oldY = Math.round(event.getY());
                        int middleY = v.getHeight() / 2;
                        int middleX = v.getWidth() / 2;
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_UP:
                                SendMessage(Command.CommandStringBuilder(Command.SPEED, "0"));
                                return false;
                            case MotionEvent.ACTION_DOWN:
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                if (event.getY() < middleY) {
                                    yDirection = Command.DIRECTION.FORWARD;
                                } else {
                                    yDirection = Command.DIRECTION.BACKWARD;
                                }
                                SendDirectionMessage(yDirection);

                                if (event.getX() > middleX + (v.getWidth() / 10)) {
                                    xDirection = Command.DIRECTION.RIGHT;
                                } else if (event.getX() < middleX - (v.getWidth() / 10)) {
                                    xDirection = Command.DIRECTION.LEFT;
                                } else {
                                    SendDirectionMessage(yDirection);
                                }

                                // if direction is forward/backward don't send left or right
                                if (xDirection != Command.DIRECTION.NONE) {
                                    SendDirectionMessage(xDirection);
                                }

                                int speed = Math.abs(Math.round(((event.getY() - middleY) / v.getHeight()) * 200));
                                if (Math.abs(speed - oldSpeed) > 3) {
                                    oldSpeed = speed;
                                    SendMessage(Command.CommandStringBuilder(Command.SPEED, String.valueOf(speed)));
                                }


                                return true;
                        }
                    } else {
                        //Log.v(TAG, "delta too low");
                        return true;
                    }
                    return false;
                }
            });
        } else {
            Log.e(TAG, "layout not found for setting touch thingey");
        }
        maxSpeed = MAXIMUM_SPEED_ALLOWED;
        tvStatus = (TextView) findViewById(R.id.tvConnectionStatus);
        tvMessage = (TextView) findViewById(R.id.tvMessage);

        messageCounter = 0;
        ConnectionManager.getInstance().setListener(new ConnectionManager.MessageCallBack() {
            @Override
            public void callBackMessageReceived(Command command, String arg) {
                parseCommand(command, arg);
                Log.v(TAG, String.format("Received command:%s with argument:%s\n", command.toString(), arg));
            }
        });
        ConnectionManager.getInstance().startMessageReceiver();
    }

    public void parseCommand(Command command, String arg) {
        switch (command) {
            case MAXSPEED:
                try {
                    maxSpeed = Integer.valueOf(arg);
                    if (maxSpeed > MAXIMUM_SPEED_ALLOWED) {
                        // or say invalid value?
                        maxSpeed = MAXIMUM_SPEED_ALLOWED;
                    }
                } catch (NumberFormatException ex) {
                    //ex.printStackTrace();
                    Log.v(TAG, "Invalid speed");
                    ShowInfo(getString(R.string.maxspeed_invalid));
                }
                break;
            case CONNECTION_ERROR:
                ShowInfo(getString(R.string.connection_lost));
                Log.v(TAG, "Connection lost");
                break;
        }
    }

    private boolean setTvMessage(String message) {
        if (tvMessage != null) {
            tvMessage.setText(message);
            return true;
        } else {
            return false;
        }
    }
    private void SendMessage(String message) {
         if (ConnectionManager.getInstance().sendMessage(message)) {
             messageCounter++;
             tvStatus.setText(getResources().getQuantityString(R.plurals.number_messages_sent, messageCounter, messageCounter));
             setTvMessage(message);
         }
    }

    private void SendDirectionMessage(Command.DIRECTION command) {
        SendMessage(Command.CommandStringBuilder(command));
    }

    private void ShowInfo(String info) {
        Assert.assertNotNull(info);
        if (tvStatus != null) {
            tvStatus.setText(info);
        }
    }
}