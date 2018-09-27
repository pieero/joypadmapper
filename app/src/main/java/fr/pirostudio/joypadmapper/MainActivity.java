package fr.pirostudio.joypadmapper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements InputManager.InputDeviceListener, BluetoothProfile.ServiceListener  {

    protected static int REQUEST_ENABLE_BT;

    protected static UUID MY_UUID;

    static {
        MY_UUID = UUID.fromString("fr.pirostudio.joypadmapper");
        REQUEST_ENABLE_BT = 1;
    }

    protected Set<BluetoothDevice> m_bt_devices;
    protected Map<String,BluetoothDevice> m_mappedDevices;
    protected Map<String,BtConnectThread> m_mappedBtConnect;
    protected Map<String,String> m_JoyToDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if ( adapter != null ) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            m_bt_devices = adapter.getBondedDevices();
            for (BluetoothDevice device : m_bt_devices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                m_mappedBtConnect.put(deviceHardwareAddress, new BtConnectThread(device));
            }

        }

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };


    public ArrayList<Integer> getGameControllerIds() {
        ArrayList<Integer> gameControllerDeviceIds = new ArrayList<Integer>();
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();

            // Verify that the device has gamepad buttons, control sticks, or both.
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                    || ((sources & InputDevice.SOURCE_JOYSTICK)
                    == InputDevice.SOURCE_JOYSTICK)) {
                // This device is a game controller. Store its device ID.
                if (!gameControllerDeviceIds.contains(deviceId)) {
                    gameControllerDeviceIds.add(deviceId);
                }
            }
        }
        return gameControllerDeviceIds;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD)
                == InputDevice.SOURCE_GAMEPAD) {
            if (event.getRepeatCount() == 0) {
                switch (keyCode) {
                    // Handle gamepad and D-pad button presses to
                    // navigate the ship

                    default:
                        if (isFireKey(keyCode)) {
                            // Update the ship object to fire lasers
                            handled = true;
                        }
                        break;
                }
            }
            if (handled) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    Dpad mDpad = new Dpad();

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        // Check if this event if from a D-pad and process accordingly.
        int deviceId = event.getDeviceId();
        String deviceIdStr = String.valueOf(deviceId);
        if ( m_JoyToDevice.containsKey(deviceIdStr) ){
            String btDeviceId = m_JoyToDevice.get(deviceIdStr);
            BtConnectThread connectThread = m_mappedBtConnect.get(btDeviceId);
            if (Dpad.isDpadDevice(event)) {

                int press = mDpad.getDirectionPressed(event);
                switch (press) {
                    case Dpad.LEFT:
                        byte test[] = {'s'};
                        connectThread.write(test);
                        // Do something for LEFT direction press

                        return true;
                    case Dpad.RIGHT:
                        // Do something for RIGHT direction press

                        return true;
                    case Dpad.UP:
                        // Do something for UP direction press

                        return true;

                }
            }
        }

        // Check if this event is from a joystick movement and process accordingly
        return false;
    }

    private static boolean isFireKey(int keyCode) {
        // Here we treat Button_A and DPAD_CENTER as the primary action
        // keys for the game.
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_BUTTON_A;
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        getShipForID(deviceId);
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        removeShipForID(deviceId);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {

    }

    private int getShipForID(int shipID) {
        int currentShip = 0;//mShips.get(shipID);
        /*
        if ( null == currentShip ) {
            currentShip = new Ship();
            mShips.append(shipID, currentShip);
        }*/
        return currentShip;
    }

    private void removeShipForID(int shipID) {
        //mShips.remove(shipID);
    }

    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        List<BluetoothDevice> devices = proxy.getConnectedDevices();
        for (BluetoothDevice device :
                devices) {
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                String name = device.getName();
                String address = device.getAddress();
            }
        }
    }

    @Override
    public  void onServiceDisconnected(int profile) {

    }

}
