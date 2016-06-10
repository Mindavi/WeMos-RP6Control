package com.example.rick.testbuttons;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by Rick on 6-6-2016.
 */
public class ConnectionManager {
    private final String TAG = "ConnectionManager";
    private final int MAX_COMMAND_LENGTH = 50;
    private final int MAX_ARG_LENGTH = 10;
    private Socket socket;
    private PrintWriter sOutput;
    private BufferedReader sInput;
    private MessageCallBack listener;
    private ConnectionCallback connectionCallback;
    private static ConnectionManager ourInstance = new ConnectionManager();
    private volatile boolean receiveMessages = false;

    public static ConnectionManager getInstance() {
        return ourInstance;
    }

    public void stopMessageReceiver() {
        receiveMessages = false;
    }

    private ConnectionManager() {
        Log.v(TAG, "ConnectionManager was created");
    }

    public void connect(String ipAddress, int port) {
        Log.v(TAG, String.format("Connect has been called with ipAddress:%s and port:%d", ipAddress, port));
        new MakeConnection().execute(ipAddress, String.valueOf(port));
    }

    public void disconnect() {
        Log.v(TAG, "Disconnect");
        try {
            if (sInput != null) {
                sInput.close();
            }
            if (sOutput != null) {
                sOutput.close();
            }
            if (socket != null && !socket.isClosed() && socket.isConnected()) {
                socket.close();
            }
        } catch (IOException e) {
            // don't worry be happy
        }
    }

    public void setListener(MessageCallBack callBack) {
        listener = callBack;
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        connectionCallback = callback;
    }

    public boolean sendMessage(String message) {
        sOutput.println('%' + message + '$');
        sOutput.flush();
        return !sOutput.checkError();
    }

    public void startMessageReceiver() {
        receiveMessages = true;
        new MessageReceiver().execute();
    }



    private class MakeConnection extends AsyncTask<String, String, ArrayList<Object>> {
        private String ipInput;
        private int portInput;

        private MakeConnection() {
            ipInput = null;
            portInput = 0;
            Log.v(TAG, "MakeConnection object created");
        }

        protected ArrayList<Object> doInBackground(String... strings) {
            Log.v(TAG, "Starting connection initiation");
            Socket tmpSocket;
            PrintWriter tmpSOutput;
            BufferedReader tmpSInput;
            ArrayList<Object> objectList = new ArrayList<>();
            ipInput = strings[0];
            portInput = Integer.parseInt(strings[1]);
            try {
                InetAddress inetAddress = InetAddress.getByName(ipInput);
                SocketAddress socketAddress = new InetSocketAddress(inetAddress, portInput);
                tmpSocket = new Socket();
                tmpSocket.connect(socketAddress, 300);
                tmpSOutput = new PrintWriter(tmpSocket.getOutputStream(), true);
                tmpSInput = new BufferedReader(new InputStreamReader(tmpSocket.getInputStream()));
            } catch (UnknownHostException e) {
                //e.printStackTrace();
                objectList.add(ConnectionConstants.UnknownHostException);
                return objectList;
            } catch (IOException e) {
                objectList.add(ConnectionConstants.IOException);
                //e.printStackTrace();
                return objectList;
            }
            objectList.add(tmpSocket);
            objectList.add(tmpSOutput);
            objectList.add(tmpSInput);
            objectList.add(ConnectionConstants.Connected);
            return objectList;
        }

        @Override
        protected void onPostExecute(ArrayList<Object> list) {
            boolean listNotEmpty = !list.isEmpty(); // empty list means exception
            int state = ConnectionConstants.Undefined;
            if (listNotEmpty && list.size() == 4) {
                socket = (Socket) list.get(0);
                sOutput = (PrintWriter) list.get(1);
                sInput = (BufferedReader) list.get(2);
                state = (int) list.get(3);
                Log.v(TAG, socket.toString());
            } else if (listNotEmpty && list.size() == 1) {
                state = (int) list.get(0);
            }
            Log.v(TAG, "got " + (state == ConnectionConstants.Connected ? "connection" : "no connection"));
            if (state == ConnectionConstants.Connected) {
                // if connected, send identification message
                sendMessage(Command.CommandStringBuilder(Command.CONTROL, "BeefburgerApp"));
            }
            if (connectionCallback != null) {
                Log.v(TAG, connectionCallback.getClass().getSimpleName());
                connectionCallback.connectionAttemptMade(state);
            } else {
                Log.v(TAG, "No connectionCallback set");
            }
        }
    }

    private class MessageReceiver extends AsyncTask<Void, String, Void> {
        protected Void doInBackground(Void... voids) {
            State state = State.wait;
            String command = "";
            String arg = "";
            if (socket == null || sInput == null || sOutput == null) {
                Log.v(TAG, "Something seems to be null");
                return null;
            }

            try {
                Log.v(TAG, String.format("ReceiveMessages:%b", receiveMessages));
                while (receiveMessages) {
                    if (socket == null || sInput == null || socket.isClosed() || !socket.isConnected()) {
                        Log.v(TAG, "Invalid something");
                        break;
                    }
                    int c = sInput.read(); // this call is blocking, so the thread does not have to sleep otherwise
                    if (c < 0) {
                        break;
                    }
                    char ch = (char) c;
                    if (sInput.ready() && c >= 0) {
                        switch (state) {
                            case wait:
                                if (ch == '%') {
                                    state = State.receiveCommand;
                                }
                                break;
                            case receiveCommand:
                                if (ch == ':') {
                                    state = State.receiveArg;
                                } else if (ch == '$') {
                                    publishProgress(command, arg);
                                    command = arg = "";
                                    state = State.wait;
                                } else {
                                    command += ch;
                                }
                                break;
                            case receiveArg:
                                if (ch == '$') {
                                    publishProgress(command, arg);
                                    command = arg = "";
                                    state = State.wait;
                                } else {
                                    arg += ch;
                                }
                        }
                        if (command.length() > MAX_COMMAND_LENGTH || arg.length() > MAX_ARG_LENGTH) {
                            publishProgress(Command.MAX_LENGTH_ERROR.toString(), "");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if (listener != null) {
                Command command;
                String arg;
                try {
                    command = Command.valueOf(values[0]);
                    arg = values[1];
                } catch (IllegalArgumentException ex) {
                    //ex.printStackTrace();
                    command = Command.INVALID_COMMAND_ERROR;
                    arg = values[0]; // so we get the command that is invalid as arg
                }
                listener.callBackMessageReceived(command, arg);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.v(TAG, "Connection lost");
            if (listener != null) {
                listener.callBackMessageReceived(Command.CONNECTION_ERROR, "");
            }
        }
    }

    private enum State {wait, receiveCommand, receiveArg};

    public interface MessageCallBack {
        void callBackMessageReceived(Command command, String arg);
    }

    public interface ConnectionCallback {
        void connectionAttemptMade(int state);
    }
}
