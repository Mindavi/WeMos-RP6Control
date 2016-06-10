package com.example.rick.testbuttons;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import junit.framework.Assert;

public class ConnectActivity extends Activity {
    private final String TAG = "ConnectActivity";
    private TextView info;
    private EditText etIpAddress;
    private EditText etPortNumber;
    private Context context;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Click!");
                String ipAddress = null;
                int portNumber = 0;
                if (etIpAddress != null && etPortNumber != null) {
                    ipAddress = etIpAddress.getText().toString();
                    try {
                        portNumber = Integer.parseInt(etPortNumber.getText().toString());
                    } catch (NumberFormatException ex) {
                        // don't care
                    }
                }

                if (ipAddress == null || ipAddress.trim().length() < 1 || portNumber < 1) {
                    System.out.println("Ip: " + (ipAddress == null ? "null" : ipAddress) + "Port: " + portNumber);
                    return;
                }
                ConnectionManager.getInstance().connect(ipAddress, portNumber);
            }
        });
        info = (TextView) findViewById(R.id.tvInfo);
        etIpAddress = (EditText) findViewById(R.id.etIpAddress);
        etPortNumber = (EditText) findViewById(R.id.etPortNumber);
        context = this;

        ConnectionManager.getInstance().setConnectionCallback(new ConnectionManager.ConnectionCallback() {
            @Override
            public void connectionAttemptMade(int state) {
                Assert.assertNotNull(info);
                switch (state) {
                    case ConnectionConstants.Connected:
                        Intent controlIntent = new Intent(context, ControlRobotActivity.class);
                        startActivity(controlIntent);
                        break;
                    case ConnectionConstants.IOException:
                        info.setText(R.string.connection_failed);
                        break;
                    case ConnectionConstants.UnknownHostException:
                        info.setText(R.string.unknown_host);
                        break;
                    case ConnectionConstants.Undefined:
                        System.out.println("Something went wrong, connectionattemp gave undefined");
                }
            }
        });
    }
}


