package com.example.rick.testbuttons;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
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
    private TextView tvStatus;
    private Socket socket;
    private int messageCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlrobot);
        tvStatus = (TextView) findViewById(R.id.tvConnectionStatus);
        socket = null;
        messageCounter = 0;
    }

    private final static int INTERVAL = 1000 * 3; //15 seconds
    Handler keepAliveHandler = new Handler();
    Runnable keepAliveHandlerTask = new Runnable()
    {
        @Override
        public void run() {
            new SendMessageOverNetwork().execute("!");
            keepAliveHandler.postDelayed(keepAliveHandlerTask, INTERVAL);
        }
    };

    void startKeepAlive()
    {
        keepAliveHandlerTask.run();
    }

    void stopKeepAlive()
    {
        keepAliveHandler.removeCallbacks(keepAliveHandlerTask);
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

    public void connect_click(View view) {
        //http://stackoverflow.com/a/33699563
        if (socket == null || socket.isClosed()) {
            new MakeSocketConnection().execute();
            startKeepAlive();
        }
    }

    private void SendMessage(String message) {
        if (socket != null && socket.isConnected()) {
            String finalMessage = "#" + message + "%";
            new SendMessageOverNetwork().execute(finalMessage);
        } else {
            tvStatus.setText(R.string.message_not_sent);
        }
    }

    private class MakeSocketConnection extends AsyncTask<Void, String, Socket> {
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
            }
        }

        protected Socket doInBackground(Void... voids) {
            try {
                ipAddress = InetAddress.getByName(ipInput);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return null;
            }
            if (ipAddress == null) {
                return null;
            }
            Socket s = new Socket();
            try {
                InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, portInput);
                publishProgress(getString(R.string.making_socket));
                s.connect(socketAddress, 300);
                return s;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                System.out.println("No connection could be established");
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("An IOException occurred");
                return null;
            }
        }
        @Override
        protected void onProgressUpdate(String... values) {
            if (tvStatus != null) {
                tvStatus.setText(values[0]);
            }
        }

        protected void onPostExecute(Socket s) {
            if (s != null) {
                setTextTvStatus(R.string.connection);
                socket = s;
            } else {
                setTextTvStatus(R.string.no_connection);
            }
        }

        private void setTextTvStatus(int stringReference) {
            if (tvStatus != null) {
                tvStatus.setText(stringReference);
            }
        }
    }

    private class SendMessageOverNetwork extends AsyncTask<String, String, Boolean> {
        protected Boolean doInBackground(String... params) {
            final String message = params[0];
            if (socket == null || message == null) {
                return false;
            }
            if (!socket.isConnected()) {
                return false;
            }
            PrintWriter sOutput;
            try {
                sOutput = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            sOutput.println(message);
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (tvStatus != null) {
                if (result) {
                    Resources res = getResources();
                    String text = res.getQuantityString(R.plurals.number_messages_sent, ++messageCounter, messageCounter);
                    tvStatus.setText(text);
                } else {
                    tvStatus.setText(R.string.message_not_sent);
                }
            }
        }
    }
}

