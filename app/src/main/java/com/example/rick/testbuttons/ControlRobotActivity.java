package com.example.rick.testbuttons;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import junit.framework.Assert;

public class ControlRobotActivity extends AppCompatActivity {
    private final String TAG = "ControlRobotActivity";
    private TextView tvStatus;
    private TextView tvSpeed;
    private int messageCounter;
    private int maxSpeed;
    private final int MAXIMUM_SPEED_ALLOWED = 130;
    private int oldSpeed = 0;
    private int oldAngle = 0;
    private Command.DIRECTION oldYDirection;
    private Command.DIRECTION oldXDirection;
    private final int MAX_ANGLE = 100;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ConnectionManager.getInstance().disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (super.isFinishing()) { // if app will be killed off, not restarted
            ConnectionManager.getInstance().disconnect();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlrobot);

        oldXDirection = Command.DIRECTION.NONE;
        oldYDirection = Command.DIRECTION.NONE;

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.ControlRobotActivity);
        if (relativeLayout != null) {
            relativeLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    final float middleY = v.getHeight() / 2;
                    final float middleX = v.getWidth() / 2;

                    Command.DIRECTION yDirection;
                    Command.DIRECTION xDirection;

                    // yDirection
                    if (event.getY() < middleY) {
                        yDirection = Command.DIRECTION.FORWARD;
                    } else {
                        yDirection = Command.DIRECTION.BACKWARD;
                    }

                    // xDirection
                    boolean setAngle;
                    if (event.getX() > middleX + (v.getWidth() / 10)) {
                        xDirection = Command.DIRECTION.RIGHT;
                        setAngle = true;
                    } else if (event.getX() < middleX - (v.getWidth() / 10)) {
                        xDirection = Command.DIRECTION.LEFT;
                        setAngle = true;
                    } else {
                        xDirection = Command.DIRECTION.MIDDLE;
                        setAngle = false;
                    }

                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_UP:
                            SendMessage(Command.CommandStringBuilder(Command.SPEED, 0));
                            return false;
                        case MotionEvent.ACTION_DOWN:
                            SendMessage(Command.CommandStringBuilder(yDirection));
                            if (xDirection != Command.DIRECTION.MIDDLE) {
                                SendMessage(Command.CommandStringBuilder(xDirection));
                            }
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (yDirection != oldYDirection) {
                                oldYDirection = yDirection;
                                SendDirectionMessage(yDirection);
                                if (xDirection != Command.DIRECTION.MIDDLE) {
                                    SendDirectionMessage(xDirection);
                                }
                            }

                            int angle = Math.round(Math.abs(event.getX() - middleX) / (v.getWidth() / 2) * MAX_ANGLE);
                            if (Math.abs(angle - oldAngle) > 5 && setAngle) {
                                SendMessage(Command.CommandStringBuilder(Command.ANGLE, angle));
                                //Log.v(TAG, String.format("Angle:%d", angle));
                                oldAngle = angle;
                            }

                            // if direction is forward/backward don't send left or right
                            if (xDirection != Command.DIRECTION.MIDDLE && xDirection != oldXDirection) {
                                SendDirectionMessage(xDirection);
                            } else if (xDirection != oldXDirection && xDirection == Command.DIRECTION.MIDDLE) { // send y direction when between left and right, thus going forward/backward
                                SendDirectionMessage(yDirection);
                            }

                            float position = Math.abs(event.getY() - middleY);
                            if (position > middleY) {
                                position = middleY;
                            }
                            int speed = Math.round(Math.round(position / (v.getHeight() / 2) * maxSpeed));
                            if (Math.abs(speed - oldSpeed) > 3) {
                                oldSpeed = speed;
                                SendMessage(Command.CommandStringBuilder(Command.SPEED, speed));
                            }

                            oldXDirection = xDirection;
                            updateSpeedString();
                            return true;
                    }
                    return false;
                }
            });
        } else {
            Log.e(TAG, "layout not found for setting touch thing");
        }
        maxSpeed = MAXIMUM_SPEED_ALLOWED;

        tvStatus = (TextView) findViewById(R.id.tvConnectionStatus);
        tvSpeed = (TextView) findViewById(R.id.tvSpeed);
        Assert.assertNotNull(tvStatus);
        Assert.assertNotNull(tvSpeed);

        messageCounter = 0;
        ConnectionManager.getInstance().setListener(new ConnectionManager.MessageCallBack() {
            @Override
            public void callBackMessageReceived(Command command, String arg) {
                parseCommand(command, arg);
                Log.v(TAG, String.format("Received command:%s with argument:%s", command.toString(), arg));
            }
        });
        ConnectionManager.getInstance().startMessageReceiver();
    }

    private void updateSpeedString() {
        tvSpeed.setText(String.format(getResources().getString(R.string.speed_param), oldYDirection == Command.DIRECTION.FORWARD ? oldSpeed : -oldSpeed, maxSpeed));
    }
    private void parseCommand(Command command, String arg) {
        switch (command) {
            case MAXSPEED:
                try {
                    maxSpeed = Integer.valueOf(arg);
                    if (maxSpeed > MAXIMUM_SPEED_ALLOWED) {
                        // or say invalid value?
                        SendMessage(Command.CommandStringBuilder(Command.INVALID_COMMAND_ERROR, maxSpeed));
                        maxSpeed = MAXIMUM_SPEED_ALLOWED;
                    }
                    updateSpeedString();
                } catch (NumberFormatException ex) {
                    //ex.printStackTrace();
                    Log.v(TAG, "Invalid speed");
                    ShowInfo(getString(R.string.max_speed_invalid));
                    SendMessage(Command.CommandStringBuilder(Command.INVALID_COMMAND_ERROR, arg));
                }
                break;
            case CONNECTION_ERROR:
                ShowInfo(getString(R.string.connection_lost));
                Log.v(TAG, "Connection lost");
                break;
        }
    }
    private void SendMessage(String message) {
         if (ConnectionManager.getInstance().sendMessage(message)) {
             messageCounter++;
             if (messageCounter % 100 == 0) {
                 Log.v(TAG, String.format("%d messages sent", messageCounter));
             }
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