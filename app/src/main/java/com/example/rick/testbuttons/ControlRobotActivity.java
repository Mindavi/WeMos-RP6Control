package com.example.rick.testbuttons;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import junit.framework.Assert;

public class ControlRobotActivity extends AppCompatActivity {
    private TextView tvStatus;
    private int messageCounter;
    private TextView tvSpeedOnBar;
    private SeekBar sbSpeed;
    private int maxSpeed;
    private final int MAXIMUMSPEEDALLOWED = 130;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlrobot);
        maxSpeed = MAXIMUMSPEEDALLOWED;
        tvStatus = (TextView) findViewById(R.id.tvConnectionStatus);
        sbSpeed = (SeekBar) findViewById(R.id.sbSpeed);
        tvSpeedOnBar = (TextView) findViewById(R.id.tvSpeedOnBar);
        if (sbSpeed != null) {
            sbSpeed.setMax(MAXIMUMSPEEDALLOWED);
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
                //ShowInfo(message);
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
                    if (maxSpeed > MAXIMUMSPEEDALLOWED) {
                        // or say invalid value?
                        maxSpeed = MAXIMUMSPEEDALLOWED;
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
        SendMessage(Command.CommandStringBuilder(Command.DIRECTION.BACKWARD));
    }

    public void right_click(View view) {
        SendMessage(Command.CommandStringBuilder(Command.DIRECTION.RIGHT));
    }

    public void left_click(View view) {
        SendMessage(Command.CommandStringBuilder(Command.DIRECTION.LEFT));
    }

    public void up_click(View view) {
        SendMessage(Command.CommandStringBuilder(Command.DIRECTION.FORWARD));
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