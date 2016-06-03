package com.example.rick.testbuttons;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ConnectionService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_SEND_MESSAGE = "com.example.rick.testbuttons.action.SEND_MESSAGE";
    public static final String ACTION_MAKE_CONNECTION = "com.example.rick.testbuttons.action.MAKE_CONNECTION";

    // TODO: Rename parameters
    public static final String EXTRA_IPADDRESS = "com.example.rick.testbuttons.extra.IPADDRESS";
    public static final String EXTRA_PORT = "com.example.rick.testbuttons.extra.PORT";
    public static final String EXTRA_MESSAGE = "com.example.rick.testbuttons.extra.MESSAGE";

    private static Socket socket;
    private static PrintWriter sOutput;

    private Context context;

    public ConnectionService() {
        super("ConnectionService");
        context = this;
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionConnect(Context context, String ipAddress, int port) {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(ACTION_MAKE_CONNECTION);
        intent.putExtra(EXTRA_IPADDRESS, ipAddress);
        intent.putExtra(EXTRA_PORT, port);
        context.startService(intent);
    }

    public static void startActionSendMessage(Context context, String message) {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(ACTION_SEND_MESSAGE);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_MAKE_CONNECTION.equals(action)) {
                final String ipAddress = intent.getStringExtra(EXTRA_IPADDRESS);
                final int port = intent.getIntExtra(EXTRA_PORT, 1);
                handleActionConnect(ipAddress, port);
            } else if (ACTION_SEND_MESSAGE.equals(action)) {
                final String message = intent.getStringExtra(EXTRA_MESSAGE);
                handleActionSendMessage(message);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionConnect(String ipAddress, int port) {
        new MakeConnection().execute(ipAddress, String.valueOf(port));
    }

    private void handleActionSendMessage(String message) {
        new SendMessageOverNetwork().execute(message);
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
            ArrayList<Object> objectList = new ArrayList<>();
            ipInput = strings[0];
            portInput = Integer.parseInt(strings[1]);
            try {
                InetAddress inetAddress = InetAddress.getByName(ipInput);
                SocketAddress socketAddress = new InetSocketAddress(inetAddress, portInput);
                tmpSocket = new Socket();
                tmpSocket.connect(socketAddress, 300);
                tmpSOutput = new PrintWriter(tmpSocket.getOutputStream(), true);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return objectList;
            } catch (IOException e) {
                e.printStackTrace();
                return objectList;
            }
            objectList.add(tmpSocket);
            objectList.add(tmpSOutput);
            return objectList;
        }

        @Override
        protected void onPostExecute(ArrayList<Object> list) {
            Intent localIntent = new Intent(ConnectionConstants.CONNECTION_BROADCAST_ACTION);
            boolean connection = !list.isEmpty();
            if (connection && list.size() == 2) {
                socket = (Socket) list.get(0);
                System.out.println(socket.toString());
                sOutput = (PrintWriter) list.get(1);
            }
            localIntent.putExtra(ConnectionConstants.EXTENDED_DATA_STATUS, (connection ? ConnectionConstants.Connected : ConnectionConstants.NotConnected));
            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
            System.out.println("sent intent and got " + (connection ? "connection" : "no connection"));
        }
    }

    private class SendMessageOverNetwork extends AsyncTask<String, String, Integer> {
        protected Integer doInBackground(String... params) {
            final String message = params[0];
            //System.out.println("Message: " + message);
            if (socket == null) {
                System.out.println("socket was null");
                return ConnectionConstants.NoSocket;
            }
            if (sOutput.checkError()) {
                //System.out.println("printwritererror");
                return ConnectionConstants.PrintWriterError;
            }
            sOutput.println(message);
            sOutput.flush();
            return ConnectionConstants.Success;
        }

        protected void onPostExecute(Integer result) {
            Intent localIntent = new Intent(ConnectionConstants.MESSAGE_BROADCAST_ACTION)
                    .putExtra(ConnectionConstants.EXTENDED_SEND_MESSAGE_DATA, result);
            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
        }
    }
}
