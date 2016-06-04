package com.example.rick.testbuttons;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class ConnectActivity extends Activity {

    TextView info;
    EditText etIpAddress;
    EditText etPortNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        IntentFilter intentFilter = new IntentFilter(
                ConnectionConstants.CONNECTION_BROADCAST_ACTION);
        ResponseReceiver responseReceiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(responseReceiver, intentFilter);
        info = (TextView) findViewById(R.id.tvInfo);
        etIpAddress = (EditText) findViewById(R.id.etIpAddress);
        etPortNumber = (EditText) findViewById(R.id.etPortNumber);
    }

    public void connect_click(View view) {
        String ipAddress = null;
        int portNumber = 0;
        if (etIpAddress != null && etPortNumber != null) {
            ipAddress = etIpAddress.getText().toString();
            portNumber = Integer.parseInt(etPortNumber.getText().toString());
        }

        if (ipAddress == null || ipAddress.trim().length() < 1 || portNumber < 1) {
            System.out.println("Ip: " + (ipAddress == null ? "null" : ipAddress) + "Port: " + portNumber);
            return;
        }

        Intent makeConnectionIntent = new Intent(this, ConnectionService.class);
        makeConnectionIntent.setAction(ConnectionService.ACTION_MAKE_CONNECTION);
        makeConnectionIntent.putExtra(ConnectionService.EXTRA_IPADDRESS, ipAddress);
        makeConnectionIntent.putExtra(ConnectionService.EXTRA_PORT, portNumber);
        startService(makeConnectionIntent);
    }

    // Broadcast receiver for receiving status updates from the IntentService
    private class ResponseReceiver extends BroadcastReceiver
    {
        private ResponseReceiver() {
        }
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            TextView tvInfo = (TextView) findViewById(R.id.tvInfo);
            int status = intent.getIntExtra(ConnectionConstants.EXTENDED_DATA_STATUS, -1);
            if (status == ConnectionConstants.Connected) {
                Intent controlIntent = new Intent(context, ControlRobotActivity.class);
                startActivity(controlIntent);
            }
            else if (status == ConnectionConstants.NotConnected) {
                if (tvInfo != null) {
                    tvInfo.setText(R.string.connection_failed);
                }
            }
            else if (status == -1) {
                throw new IllegalStateException("should not be -1 in ResponseReceiver");
            }
        }
    }
}


