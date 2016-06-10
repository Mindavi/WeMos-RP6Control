package com.example.rick.testbuttons;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import junit.framework.Assert;

public class ControlRobotActivity extends AppCompatActivity {
    private TextView tvStatus;
    private int messageCounter;
    private TextView tvSpeedOnBar;
    private Button btnLeft;
    private Button btnRight;
    private SeekBar sbSpeed;
    private int maxSpeed;
    private final int MAXIMUM_SPEED_ALLOWED = 130;
    private Command.DIRECTION verticalDirection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlrobot);
        maxSpeed = MAXIMUM_SPEED_ALLOWED;
        tvStatus = (TextView) findViewById(R.id.tvConnectionStatus);
        sbSpeed = (SeekBar) findViewById(R.id.sbSpeed);
        tvSpeedOnBar = (TextView) findViewById(R.id.tvSpeedOnBar);

        btnLeft = (Button) findViewById(R.id.btnLeft);
        btnRight = (Button) findViewById(R.id.btnRight);

        btnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        SendMessage(Command.CommandStringBuilder(Command.DIRECTION.LEFT));
                        return true;
                    case MotionEvent.ACTION_UP:
                        SendMessage(Command.CommandStringBuilder(verticalDirection));
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        SendMessage(Command.CommandStringBuilder(verticalDirection));
                        return false;
                }
                return false;
            }
        });

        btnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        SendMessage(Command.CommandStringBuilder(Command.DIRECTION.RIGHT));
                        return true;
                    case MotionEvent.ACTION_UP:
                        SendMessage(Command.CommandStringBuilder(verticalDirection));
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        SendMessage(Command.CommandStringBuilder(verticalDirection));
                        return false;
                }
                return false;
            }
        });

        if (sbSpeed != null) {
            sbSpeed.setMax(MAXIMUM_SPEED_ALLOWED);
            sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    Assert.assertNotNull(tvSpeedOnBar);
                    Assert.assertNotNull(sbSpeed);
                    if (progress > maxSpeed) {
                        sbSpeed.setProgress(maxSpeed);
                    }
                    if (tvSpeedOnBar != null) {
                        updateSpeedString();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int speed = seekBar.getProgress();
                    SendMessage(String.format("%s:%s", Command.SPEED.toString(), Integer.toString(speed)));
                }
            });
        }
        messageCounter = 0;
        ConnectionManager.getInstance().setListener(new ConnectionManager.MessageCallBack() {
            @Override
            public void callBackMessageReceived(Command command, String arg) {
                parseCommand(command, arg);
                System.out.printf("Received command:%s with argument:%s\n", command.toString(), arg);
            }
        });
        ConnectionManager.getInstance().startMessageReceiver();
        updateSpeedString();
    }

    private void updateSpeedString() {
        Assert.assertNotNull(sbSpeed);
        Assert.assertNotNull(tvSpeedOnBar);
        int speed = sbSpeed.getProgress();
        String sSpeed = String.format(getResources().getString(R.string.speed_param), speed, maxSpeed);
        tvSpeedOnBar.setText(sSpeed);
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
                    int setSpeed = sbSpeed.getProgress();
                    if (setSpeed > maxSpeed) {
                        sbSpeed.setProgress(maxSpeed);
                    }
                    updateSpeedString();
                } catch (NumberFormatException ex) {
                    //ex.printStackTrace();
                    System.out.println("Invalid speed");
                    ShowInfo(getString(R.string.maxspeed_invalid));
                }
                break;
            case CONNECTION_ERROR:
                ShowInfo(getString(R.string.connection_lost));
                break;
        }
    }

    public void down_click(View view) {
        verticalDirection = Command.DIRECTION.BACKWARD;
        SendMessage(Command.CommandStringBuilder(verticalDirection));
    }

    public void up_click(View view) {
        verticalDirection = Command.DIRECTION.FORWARD;
        SendMessage(Command.CommandStringBuilder(verticalDirection));

    }

    private void SendMessage(String message) {
         if (ConnectionManager.getInstance().sendMessage(message)) {
             messageCounter++;
             tvStatus.setText(getResources().getQuantityString(R.plurals.number_messages_sent, messageCounter, messageCounter));
         }
    }

    private void ShowInfo(String info) {
        Assert.assertNotNull(info);
        if (tvStatus != null) {
            tvStatus.setText(info);
        }
    }
}