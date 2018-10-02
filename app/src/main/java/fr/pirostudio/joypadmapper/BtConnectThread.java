package fr.pirostudio.joypadmapper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static fr.pirostudio.joypadmapper.MainActivity.MY_UUID;

public class BtConnectThread extends Thread {
    private static final String TAG = "JoypadMapper";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public final ObservableField<String> name = new ObservableField<>();
    public final ObservableBoolean disconnected = new ObservableBoolean();

    MainActivity.BluetoothHandler mHandler;

        public BtConnectThread(BtConnectThread copy) {
            mmDevice = copy.mmDevice;
            disconnected.set(true);
            name.set(mmDevice.getName());
            mHandler = copy.mHandler;
            mmSocket = copy.mmSocket;
            mmInStream = copy.mmInStream;
            mmOutStream = copy.mmOutStream;
        }

        public BtConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            mmDevice = device;
            disconnected.set(true);
            name.set(device.getName());

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

    public String getAddress() {
            return mmDevice.getAddress();
    }

    public void setHandler(MainActivity.BluetoothHandler handler)
    {
        mHandler = handler;
    }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            boolean stop = false;
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                disconnected.set(false);
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                mHandler.obtainMessage(MainActivity.BluetoothHandler.MESSAGE_DISCONNECTED, mmDevice.getAddress()).sendToTarget();
                disconnected.set(true);
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

            mHandler.obtainMessage(MainActivity.BluetoothHandler.MESSAGE_CONNECTED, mmDevice.getAddress()).sendToTarget();

            while ( mmSocket.isConnected() )
            {
                try{
                    wait(1000);
                }
                catch(Exception e)
                {
                    break;
                }
            }
            disconnected.set(true);
            mHandler.obtainMessage(MainActivity.BluetoothHandler.MESSAGE_DISCONNECTED, mmDevice.getAddress()).sendToTarget();
           //manageMyConnectedSocket(mmSocket);
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