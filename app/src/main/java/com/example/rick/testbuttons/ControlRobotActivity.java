package com.example.rick.testbuttons;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ControlRobotActivity extends AppCompatActivity {
    private TextView tvDirection = null;
    private TextView tvStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlrobot);
        tvDirection = (TextView) findViewById(R.id.tvDirectionPressed);
        tvStatus = (TextView) findViewById(R.id.tvConnectionStatus);
    }

    public void down_click(View view) {
        UpdateTextViewAndSendMessage("BACKWARD");
    }

    public void right_click(View view) {
        UpdateTextViewAndSendMessage("RIGHT");
    }

    public void left_click(View view) {
        UpdateTextViewAndSendMessage("LEFT");
    }

    public void up_click(View view) {
        UpdateTextViewAndSendMessage("FORWARD");
    }

    private void UpdateTextViewAndSendMessage(String message) {
        tvDirection.setText(message);
        new SendMessageOverNetwork().execute(message);
    }

    private class SendMessageOverNetwork extends AsyncTask<String, String, Boolean> {
        private InetAddress ipAddress = null;
        private String ipInput = null;
        private int portInput = 0;

        @Override
        protected void onPreExecute() {
            EditText tbIpAddress = (EditText) findViewById(R.id.tbIpAddress);
            EditText tbPortNumber = (EditText) findViewById(R.id.tbPortNumber);
            String portString = null;
            if (tbIpAddress != null) {
                ipInput = tbIpAddress.getText().toString();
            }
            if (tbPortNumber != null) {
                portString = tbPortNumber.getText().toString();
            }
            try {
                portInput = Integer.parseInt(portString);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                TextView connStatus = (TextView) findViewById(R.id.tvConnectionStatus);
                if (connStatus != null) {
                    connStatus.setText(R.string.invalid_port_number);
                }
                cancel(true);
            }
        }

        protected Boolean doInBackground(String... params) {
            try {
                ipAddress = InetAddress.getByName(ipInput);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return false;
            }
            if (ipAddress == null) {
                return false;
            }
            final String message = params[0];
            final Socket s = new Socket();
            try {
                InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, portInput);
                publishProgress(getString(R.string.making_socket));
                s.connect(socketAddress, 300);
                {
                    publishProgress(getString(R.string.connection));
                }
                PrintWriter outp = null;
                try {
                    outp = new PrintWriter(s.getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (message != null && outp != null) {
                    outp.println(message);
                }
                s.close();
                return true;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                System.out.println("No connection could be established");
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("An IOException occurred");
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (tvStatus != null) {
                tvStatus.setText(values[0]);
            }
        }

        protected void onPostExecute(Boolean result) {
            if (tvStatus != null) {
                if (result) {
                    tvStatus.setText(R.string.message_sent);
                } else {
                    tvStatus.setText(R.string.connection_failed);
                }
            }
        }
    }
}

