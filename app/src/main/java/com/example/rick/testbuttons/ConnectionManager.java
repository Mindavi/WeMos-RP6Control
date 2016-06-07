package com.example.rick.testbuttons;

import android.os.AsyncTask;

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
    private final int MAXCOMMANDLENGTH = 50;
    private final int MAXARGLENGTH = 10;
    private Socket socket;
    private PrintWriter sOutput;
    private BufferedReader sInput;
    private MessageCallBack listener;
    private ConnectionCallback connectionCallback;
    private static ConnectionManager ourInstance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return ourInstance;
    }

    private ConnectionManager() {
    }

    public void connect(String ipAddress, int port) {
        System.out.printf("Connect has been called with ipAddress:%s and port:%d", ipAddress, port);
        new MakeConnection().execute(ipAddress, String.valueOf(port));
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
        new MessageReceiver().execute();
    }



    private class MakeConnection extends AsyncTask<String, String, ArrayList<Object>> {
        private String ipInput;
        private int portInput;

        private MakeConnection() {
            ipInput = null;
            portInput = 0;
        }

        protected ArrayList<Object> doInBackground(String... strings) {

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
                System.out.println(socket.toString());
            } else if (listNotEmpty && list.size() == 1) {
                state = (int) list.get(0);
            }
            System.out.println("got " + (state == ConnectionConstants.Connected ? "connection" : "no connection"));
            if (state == ConnectionConstants.Connected) {
                // if connected, send identification message
                sendMessage(Command.CommandStringBuilder(Command.CONTROL, "BestuurderApp"));
            }
            if (connectionCallback != null) {
                connectionCallback.connectionAttemptMade(state);
            }
        }
    }

    private class MessageReceiver extends AsyncTask<Void, String, Void> {

        protected Void doInBackground(Void... voids) {
            State state = State.wait;
            String command = "";
            String arg = "";
            if (socket == null) {
                return null;
            }

            try {
                while (true) {
                    if (sInput == null) {
                        break;
                    }
                    int c = sInput.read();
                    if (c < 0) {
                        break;
                    }
                    char ch = (char) c;
                    if (sInput.ready() && c >= 0) {
                        switch (state) {
                            case wait:
                                if (ch == '%') {
                                    state = State.receiveCommand;
                                    System.out.println("receiving...");
                                }
                                break;
                            case receiveCommand:
                                if (ch == ':') {
                                    state = State.receiveArg;
                                }
                                else if (ch == '$') {
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
                        if (command.length() > MAXCOMMANDLENGTH || arg.length() > MAXARGLENGTH) {
                            publishProgress(Command.MAX_LENGTH_ERROR.toString(), "");
                        }
                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
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
            System.out.println("Connection lost");
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
