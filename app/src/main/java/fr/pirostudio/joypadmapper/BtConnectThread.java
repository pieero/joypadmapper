package fr.pirostudio.joypadmapper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BtConnectThread extends BaseObservable {
    private static final String TAG = "JoypadMapper";
    private final BluetoothDevice mmDevice;

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

        private BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        protected final UUID MY_UUID;

        public ConnectionThread() {
            BluetoothSocket tmp = null;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

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

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            boolean stop = false;
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                updateStatusInfo(STATUS_CONNECTING);
                //BluetoothServerSocket bluesocketserver = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord("", MY_UUID);
                //mmSocket = bluesocketserver.accept();

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

        public boolean isConnected() {
            boolean retVal = false;
            if ( mmSocket !=  null )
            {
                retVal = mmSocket.isConnected();
            }
            return retVal;
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

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
/*
private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice device = mmDevice;//intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    Log.d("Test", "DVC CON " + device.getAddress());
                    if (device.getAddress().equals("98:D3:31:FB:2A:CE")) {
                        BluetoothSocket socket = null;
                        String data = "/0";
                        byte[] bytes = data.getBytes();
                        try {
                            BluetoothServerSocket bluesocketserver = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("", BTModuleUUID);
                            socket = bluesocketserver.accept();
                            if(socket.isConnected())
                                socket.getOutputStream().write(bytes);
                        } catch (IOException e) {
                            Log.d("Test", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        };*/
    }

    private ConnectionThread thread;

        public BtConnectThread(BluetoothDevice device, Context context) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            mmContext = context;
            mmDevice = device;
            name.set(device.getName());
            updateStatusInfo(STATUS_DISCONNECTED);

    }

    public void start() {
            if ( thread == null )
            {
                thread = new ConnectionThread();
            }
            if ( thread.getState() == Thread.State.NEW && thread.isConnected() == false )
            {
                thread.start();
            }
            else if ( thread.getState() == Thread.State.RUNNABLE ||
                    thread.getState() == Thread.State.WAITING ||
                    thread.getState() == Thread.State.TIMED_WAITING ||
                    thread.getState() == Thread.State.BLOCKED ||
                    thread.isConnected() )
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

    public void write(byte[] bytes) {
            if ( thread != null && thread.isConnected() )
            {
                thread.write(bytes);
            }
    }

        public boolean isConnected() {
            boolean retVal = false;
            if ( thread !=  null )
            {
                retVal = thread.isConnected();
            }
            return retVal;
    }

    public void setHandler(BluetoothHandler handler)
    {
        mHandler = handler;
    }


    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        if ( thread !=  null )
            thread.cancel();
    }
}