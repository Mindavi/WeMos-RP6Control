package com.example.rick.testbuttons;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class ControlRobotActivity extends AppCompatActivity {
    private TextView tvStatus;
    private Button btnBind;
    private SocketAddress socketAddress;
    private int messageCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlrobot);
        tvStatus = (TextView) findViewById(R.id.tvConnectionStatus);
        btnBind = (Button) findViewById(R.id.btnBind);
        socketAddress = null;
        messageCounter = 0;
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

    public void bind_click(View view) {
        if (socketAddress != null) {
            socketAddress = null;
            tvStatus.setText(R.string.unbind_done);
            btnBind.setText(R.string.bind);
            messageCounter = 0;
        } else {
            new MakeSocketAddress().execute();
        }
    }

    private void SendMessage(String message) {
        String finalMessage = "#" + message + "%";
        new SendMessageOverNetwork().execute(finalMessage);
    }

    private class MakeSocketAddress extends AsyncTask<Void, String, InetSocketAddress> {
        private String ipInput;
        private int portInput;

        private MakeSocketAddress() {
            ipInput = null;
            portInput = 0;
        }

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

        protected InetSocketAddress doInBackground(Void... voids) {
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(ipInput);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return null;
            }
            return new InetSocketAddress(inetAddress, portInput);
        }

        protected void onPostExecute(InetSocketAddress s) {
            if (s != null) {
                socketAddress = s;
                tvStatus.setText(R.string.socket_set);
                btnBind.setText(R.string.unbind);
            } else {
                btnBind.setText(R.string.bind);
                tvStatus.setText(R.string.socket_set_failed);
            }
        }
    }

    private class SendMessageOverNetwork extends AsyncTask<String, String, Integer> {
        final int SUCCESS = 1;
        final int IOEXCEPTION = -1;
        final int NOSOCKET = -2;
        protected Integer doInBackground(String... params) {
            final String message = params[0];
            if (socketAddress == null) {
                return NOSOCKET;
            }
            try {
                Socket socket = new Socket();
                socket.connect(socketAddress, 300);
                PrintWriter sOutput;
                sOutput = new PrintWriter(socket.getOutputStream(), true);
                sOutput.println(message);
                sOutput.flush();

                sOutput.close(); //not sure if needed
                socket.close();
                return SUCCESS;
            } catch (IOException e) {
                e.printStackTrace();
                return IOEXCEPTION;
            }

        }

        protected void onPostExecute(Integer result) {
            if (tvStatus != null) {
                switch(result) {
                    case SUCCESS:
                        String text = getResources().getQuantityString(R.plurals.number_messages_sent, ++messageCounter, messageCounter);
                        tvStatus.setText(text);
                        break;
                    case IOEXCEPTION:
                        tvStatus.setText(R.string.connection_failed);
                        break;
                    case NOSOCKET:
                        tvStatus.setText(R.string.no_socket_set);
                        break;
                }
            }
        }
    }
}

