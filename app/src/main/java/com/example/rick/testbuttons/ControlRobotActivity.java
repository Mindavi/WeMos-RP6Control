package com.example.rick.testbuttons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import junit.framework.Assert;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Locale;

public class ControlRobotActivity extends AppCompatActivity {
    private TextView tvStatus;
    private SeekBar sbSpeed;
    private SocketAddress socketAddress;
    private int messageCounter;
    private TextView tvSpeedOnBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlrobot);
        tvStatus = (TextView) findViewById(R.id.tvConnectionStatus);
        sbSpeed = (SeekBar) findViewById(R.id.sbSpeed);
        tvSpeedOnBar = (TextView) findViewById(R.id.tvSpeedOnBar);
        if (sbSpeed != null) {
            sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (tvSpeedOnBar != null) {
                        String sSpeed = String.format(getResources().getString(R.string.speed_param), progress);
                        tvSpeedOnBar.setText(sSpeed);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int speed = seekBar.getProgress();
                    SendMessage("SPEED:" + Integer.toString(speed));
                }
            });
        }
        socketAddress = null;
        messageCounter = 0;
        IntentFilter intentFilter = new IntentFilter(
                ConnectionConstants.MESSAGE_BROADCAST_ACTION);
        ResponseReceiver responseReceiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(responseReceiver, intentFilter);
    }

    public void down_click(View view) {
        SendMessage("BACKWARD");
    }

    public void right_click(View view) {
        SendMessage("RIGHT");
    }

    public void left_click(View view) {
        SendMessage("LEFT");
    }

    public void up_click(View view) {
        SendMessage("FORWARD");
    }

    private void SendMessage(String message) {
        String finalMessage = "#" + message + "%";
        Intent makeConnectionIntent = new Intent(this, ConnectionService.class);
        makeConnectionIntent.setAction(ConnectionService.ACTION_SEND_MESSAGE);
        makeConnectionIntent.putExtra(ConnectionService.EXTRA_MESSAGE, finalMessage);
        startService(makeConnectionIntent);
    }

    private void ShowInfo(String info) {
        Assert.assertNotNull(info);
        if (tvStatus != null) {
            tvStatus.setText(info);
        }
    }

    // Broadcast receiver for receiving status updates from the IntentService
    private class ResponseReceiver extends BroadcastReceiver
    {
        private ResponseReceiver() {
        }
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            TextView tvInfo = (TextView) findViewById(R.id.tvInfo);
            int status = intent.getIntExtra(ConnectionConstants.EXTENDED_SEND_MESSAGE_DATA, -1);
            if (status == ConnectionConstants.Success) {
                messageCounter++;
                String messagesSent = getResources().getQuantityString(R.plurals.number_messages_sent, messageCounter, messageCounter);
                ShowInfo(messagesSent);
            }
            else if (status == ConnectionConstants.NoSocket) {
                ShowInfo(getString(R.string.no_socket_set));
            }
            else if (status == ConnectionConstants.PrintWriterError) {
                // retry connecting?
                System.out.println("Error with printwriter");
                ShowInfo(getString(R.string.connection_lost));


            }
            else if (status == -1) {
                throw new IllegalStateException("should not be -1 in ResponseReceiver");
            }
        }
    }
}