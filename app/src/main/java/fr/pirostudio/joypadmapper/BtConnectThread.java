package fr.pirostudio.joypadmapper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static fr.pirostudio.joypadmapper.MainActivity.MY_UUID;

public class BtConnectThread extends BaseObservable {
    private static final String TAG = "JoypadMapper";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private final Context mmContext;

    static class StatusInfo {
        public final int name;
        public final int action;
        public final boolean actionable;

        public StatusInfo(int name, int action, boolean actionable) {
            this.name = name;
            this.action = action;
            this.actionable = actionable;
        }
    }

    public final ObservableField<String> name = new ObservableField<>();
    public final ObservableBoolean actionable = new ObservableBoolean();
    public final ObservableField<String> status = new ObservableField<>();
    public final ObservableField<String> action = new ObservableField<>();
    private int gamepadIndex = -1;

    @Bindable({"gamepadIndex"})
    public int getGamepadIndex() {
        return gamepadIndex;
    }

    @Bindable({"gamepadIndex"})
    public void setGamepadIndex(int index) {
        gamepadIndex = index;
        UsbLinkedObject obj = new UsbLinkedObject(index,mmDevice.getAddress());
        mHandler.obtainMessage(BluetoothHandler.MESSAGE_USB_LINKED, obj).sendToTarget();
    }

    BluetoothHandler mHandler;

    public static StatusInfo STATUS_CONNECTED = new StatusInfo(R.string.Connected, R.string.Disconnected, true);
    public static StatusInfo STATUS_CONNECTING = new StatusInfo(R.string.Connecting, R.string.Connecting, false);
    public static StatusInfo STATUS_DISCONNECTED = new StatusInfo(R.string.Disconnected, R.string.Connect, true);

    protected void updateStatusInfo(StatusInfo info)
    {
        status.set(mmContext.getResources().getString(info.name));
        action.set(mmContext.getResources().getString(info.action));
        actionable.set(info.actionable);
    }

    public class ConnectionThread extends Thread {

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            boolean stop = false;
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                updateStatusInfo(STATUS_CONNECTING);
                mmSocket.connect();
                updateStatusInfo(STATUS_CONNECTED);
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                updateStatusInfo(STATUS_DISCONNECTED);
                mHandler.obtainMessage(BluetoothHandler.MESSAGE_DISCONNECTED, mmDevice.getAddress()).sendToTarget();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                Log.e(TAG, "Failed to connect !!!!" );
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Log.i(TAG, "Connection done !!!!" );

            mHandler.obtainMessage(BluetoothHandler.MESSAGE_CONNECTED, mmDevice.getAddress()).sendToTarget();

            while ( mmSocket.isConnected() )
            {
                try{
                    wait(500);
                }
                catch(Exception e)
                {
                    break;
                }
            }
            updateStatusInfo(STATUS_DISCONNECTED);
            mHandler.obtainMessage(BluetoothHandler.MESSAGE_DISCONNECTED, mmDevice.getAddress()).sendToTarget();
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            //manageMyConnectedSocket(mmSocket);
        }
    }

    private ConnectionThread thread;

        public BtConnectThread(BluetoothDevice device, Context context) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            mmContext = context;
            mmDevice = device;
            name.set(device.getName());
            updateStatusInfo(STATUS_DISCONNECTED);
            BluetoothSocket tmp = null;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;

            if ( mmSocket != null ) {
                try {
                    tmpIn = mmSocket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }
                try {
                    tmpOut = mmSocket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                }
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

    }

    public void start() {
            if ( thread == null )
            {
                thread = new ConnectionThread();
            }
            if ( thread.getState() == Thread.State.NEW && mmSocket.isConnected() == false )
            {
                thread.start();
            }
            else if ( thread.getState() == Thread.State.RUNNABLE ||
                    thread.getState() == Thread.State.WAITING ||
                    thread.getState() == Thread.State.TIMED_WAITING ||
                    thread.getState() == Thread.State.BLOCKED ||
                    mmSocket.isConnected() )
            {
                cancel();
                thread = new ConnectionThread();
                thread.start();
            }
            else
            {
                thread = new ConnectionThread();
                thread.start();
            }
    }

    public String getAddress() {
            return mmDevice.getAddress();
    }

    public boolean isConnected() {
            boolean retVal = false;
            if ( mmSocket !=  null )
            {
                retVal = mmSocket.isConnected();
            }
            return retVal;
    }

    public void setHandler(BluetoothHandler handler)
    {
        mHandler = handler;
    }

    public void write(byte[] bytes) {
        try {
            if ( mmSocket.isConnected() )
            {
                mmOutStream.write(bytes);
            }
/*
            // Share the sent message with the UI activity.
            Message writtenMsg = mHandler.obtainMessage(
                    MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();*/
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);
/*
            // Send a failure message back to the activity.
            Message writeErrorMsg =
                    mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            mHandler.sendMessage(writeErrorMsg);*/
        }
    }

    // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }