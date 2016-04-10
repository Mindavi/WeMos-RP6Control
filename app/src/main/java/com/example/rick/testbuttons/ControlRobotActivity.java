package com.example.rick.testbuttons;

import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
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
    protected TextView tvDirection = null;
    protected TextView tvStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlrobot);
        tvDirection = (TextView) findViewById(R.id.tvDirectionPressed);
        tvStatus = (TextView) findViewById(R.id.tvConnectionStatus);
    }

    public void down_click(View view) {
        UpdateTextViewAndSendMessage("DOWN");
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
        //final private byte[] IPADDRESS = {10, 0, 2, 2};
        private InetAddress ipAddress = null;
        private String input = null;

        @Override
        protected void onPreExecute() {
            EditText editText = (EditText) findViewById(R.id.editText);
            if (editText != null) {
                input = editText.getText().toString();
            }
        }

        protected Boolean doInBackground(String... params) {
            try {
                ipAddress = InetAddress.getByName(input);
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
                InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, 80);
                publishProgress("Making socket...");
                s.connect(socketAddress, 100);
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

