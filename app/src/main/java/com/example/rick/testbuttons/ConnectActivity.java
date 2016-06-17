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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        etIpAddress = (EditText) findViewById(R.id.etIpAddress);
        etPortNumber = (EditText) findViewById(R.id.etPortNumber);
        Button btnConnect = (Button) findViewById(R.id.btnConnect);
        info = (TextView) findViewById(R.id.tvInfo);
        Assert.assertNotNull(etIpAddress);
        Assert.assertNotNull(etPortNumber);
        Assert.assertNotNull(btnConnect);
        Assert.assertNotNull(info);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectionManager.getInstance().stopMessageReceiver();
                String ipAddress;
                int portNumber = 0;
                ipAddress = etIpAddress.getText().toString();
                try {
                    portNumber = Integer.parseInt(etPortNumber.getText().toString());
                } catch (NumberFormatException ex) {
                    info.setText(R.string.invalid_port_number);
                }

                if (ipAddress.trim().length() < 1 || portNumber < 1) {
                    Log.v(TAG, "Ip: " + ipAddress + " Port: " + portNumber);
                    return;
                }
                ConnectionManager.getInstance().connect(ipAddress, portNumber);
            }
        });
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
                        Log.v(TAG, "Something went wrong, connection attempt gave undefined");
                        break;
                    default:
                        Log.v(TAG, "Shouldn't ever happen");
                        break;
                }
            }
        });
    }
}


