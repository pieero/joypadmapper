package fr.pirostudio.joypadmapper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableField;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabHost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fr.pirostudio.joypadmapper.databinding.ActivityMainBinding;
import fr.pirostudio.joypadmapper.databinding.BluetoothComViewBinding;

public class MainActivity extends AppCompatActivity implements InputManager.InputDeviceListener, BluetoothProfile.ServiceListener  {

    public final ObservableField<String> bluetoothStat = new ObservableField<>();
    public final ObservableField<String> usbStat = new ObservableField<>();
    public final ObservableField<String> usbLog = new ObservableField<>();
    public final ObservableField<String> usbLog2 = new ObservableField<>();
    public final ObservableArrayList<String> gamepad_list = new ObservableArrayList<>();
    public final ArrayList<Integer> gamepadId_list = new ArrayList<>();

    public List<InputDevice> m_connectedPads;
    public Map<Integer, GamePadMapper> m_mappedPads;

    protected static int REQUEST_ENABLE_BT;

    protected static UUID MY_UUID;

    static {
        MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        REQUEST_ENABLE_BT = 1;
    }

    protected Set<BluetoothDevice> m_bt_devices;
    protected Map<String,BluetoothDevice> m_mappedDevices;
    protected Map<String,BtConnectThread> m_mappedBtConnect;
    protected Map<String,String> m_JoyToDevice;
    protected Map<Integer,String> m_usbId_to_btAddress;
    protected Handler usbDeviceHandler;

    class UsbDeviceHandler extends Handler {

        public static final int MESSAGE_CONNECTED = 0;
        public static final int MESSAGE_DISCONNECTED = 1;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int deviceId = msg.arg1;
            switch (msg.what) {
                case MESSAGE_CONNECTED:
                    m_mappedPads.put(deviceId, new GamePadMapper(InputDevice.getDevice(deviceId)));
                    break;
                case MESSAGE_DISCONNECTED:
                    m_mappedPads.remove(deviceId);
                    break;
            }
            usbStat.set(String.valueOf(m_mappedPads.size()) + " gamepads2");
            //switch (msg.what) {
        }
    }

    ActivityMainBinding binding;

    BluetoothHandler btHandler = new BluetoothHandler(this);
    UsbDeviceHandler usbHandler = new UsbDeviceHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_mappedDevices = new HashMap<>();
        m_mappedBtConnect = new HashMap<>();
        m_JoyToDevice = new HashMap<>();

        m_connectedPads = new ArrayList<>();
        m_mappedPads = new HashMap<>();
        m_usbId_to_btAddress = new HashMap<>();
        usbLog.set("Nothing");

        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        binding.setModel(this);

        Button butTop = (Button)findViewById(R.id.buttonUp);
        butTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //usbLog.set("buttonTop");
                for(BtConnectThread t: m_mappedBtConnect.values())
                {
                    t.write("a 10\n".getBytes());
                }
            }
        });

        Button butLeft = (Button)findViewById(R.id.buttonLeft);
        butLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //usbLog.set("buttonLeft");
                for(BtConnectThread t: m_mappedBtConnect.values())
                {
                    t.write("a 10\n".getBytes());
                }
            }
        });

        TabHost host = (TabHost)findViewById(android.R.id.tabhost);
        host.setup();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if ( adapter != null ) {
            // ask for activation of bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            m_bt_devices = adapter.getBondedDevices();
            for (BluetoothDevice device : m_bt_devices) {
                BluetoothClass btClass = device.getBluetoothClass();
                boolean networking = btClass.hasService(BluetoothClass.Service.NETWORKING);
                Log.d("t", "onCreate: btClass = '"+btClass.toString()+"'");
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BtConnectThread btConnect = new BtConnectThread(device, MainActivity.this);
                btConnect.setHandler(btHandler);

                TabHost.TabSpec spec = host.newTabSpec(device.getName());
                BluetoothComViewBinding btComBinding = DataBindingUtil.inflate( getLayoutInflater(),R.layout.bluetooth_com_view,null,true);
                btComBinding.setItem(btConnect);
                btComBinding.setModel(this);
                View bluetoothComView = btComBinding.getRoot();

                Button connectButton = bluetoothComView.findViewById(R.id.ConnectButton);
                connectButton.setTag(device.getAddress());
                connectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String btAddress = (String)v.getTag();
                        try {
                            BtConnectThread thr = m_mappedBtConnect.get(btAddress);
                            if ( thr.isConnected() ) {
                                thr.cancel();
                            }
                            else{
                                thr.start();
                            }
                        }
                        catch(Exception e) {

                        }
                    }
                });

                class MyFactory implements TabHost.TabContentFactory {
                    private final View v;
                    public MyFactory(View pv) { v = pv; }
                    public View createTabContent(String tag) { return v; }
                }
                spec.setContent(new MyFactory(bluetoothComView));

                spec.setIndicator(device.getName());
                host.addTab(spec);

                //thread.start();
                //thread.write(hello);
                m_mappedBtConnect.put(deviceHardwareAddress, btConnect );
            }

            //host.setup();

            bluetoothStat.set(String.valueOf(m_bt_devices.size()) + " bluetooth");

        }
        else
        {
            bluetoothStat.set("disabled");
        }

        ArrayList<Integer> usbDevices = getGameControllerIds();
        usbStat.set(String.valueOf(usbDevices.size()) + " gamepads");

        gamepad_list.clear();
        gamepadId_list.clear();
        gamepad_list.add(this.getResources().getString(R.string.NoGamepad));
        gamepadId_list.add(-1);
        for(Integer usbId: usbDevices)
        {
            InputDevice dev = InputDevice.getDevice(usbId);
            gamepad_list.add(usbName(dev));
            gamepadId_list.add(usbId);
        }


        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        im.getInputDeviceIds();
        im.registerInputDeviceListener((InputManager.InputDeviceListener)this, null);

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
                if (!m_mappedPads.containsKey(deviceId)) {
                    m_mappedPads.put(deviceId, new GamePadMapper(dev));
                    gameControllerDeviceIds.add(deviceId);
                }
            }
        }
        return gameControllerDeviceIds;
    }

    KeyEventEx mDpad = new KeyEventEx();

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        boolean retVal = false;
        int press = mDpad.getDirectionPressed(ev);
        //if ( press != -1 )
        {
            //usbLog2.set(KeyEventEx.keyCodeToString(press));
            Set<Integer> axis = KeyEventEx.getAxisInMotion(ev);
            String axisStr = "";
            for(Integer i: axis)
            {
                axisStr += MotionEvent.axisToString(i) + ":";
                axisStr += (int)(ev.getAxisValue(i) * 10.) + " ";
            }
            usbLog2.set(axisStr);
        }
        if ( m_usbId_to_btAddress.containsKey(ev.getDeviceId()) && m_mappedPads.containsKey(ev.getDeviceId()) ) {
            if (KeyEventEx.isDpadDevice(ev)) {
                m_mappedPads.get(ev.getDeviceId()).onKeyDown(press);
            }
            retVal = true;
        } else {
            retVal = super.dispatchGenericMotionEvent(ev);
        }
        return retVal;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        boolean retVal = false;
        int keyCode = ev.getKeyCode();
        if ( ev.getAction() == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_DPAD_CENTER ) {
            usbLog.set(KeyEvent.keyCodeToString(keyCode));
        }
        if ( m_usbId_to_btAddress.containsKey(ev.getDeviceId()) && m_mappedPads.containsKey(ev.getDeviceId()) ) {
            GamePadMapper mapper = m_mappedPads.get(ev.getDeviceId());
            if ( mapper.handleKey(keyCode) ) {
                if (ev.getAction() == KeyEvent.ACTION_DOWN) {
                    retVal = mapper.onKeyDown(keyCode);
                }
                if (ev.getAction() == KeyEvent.ACTION_UP) {
                    retVal = mapper.onKeyUp(keyCode);
                }
            } else {
                // Add to view and to map
                View linkView = getLayoutInflater().inflate(R.layout.gamepad_key_link, null);
                Button keyButton = linkView.findViewById(R.id.button_key);
                keyButton.setText(KeyEvent.keyCodeToString(keyCode));
                Button linkButton = linkView.findViewById(R.id.button_key);
                linkButton.setText(R.string.link);
            }

        } else {
            retVal = super.dispatchKeyEvent(ev);
        }
        return retVal;
    }

    protected String usbName(InputDevice dev) {
        return dev.getName()+"("+String.valueOf(dev.getId())+")";
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        boolean found = false;
        for(String s:gamepad_list)
        {
            if ( s.startsWith(String.valueOf(deviceId)+":") )
            {
                found = true;
            }
        }
        if (! found )
        {
            InputDevice dev = InputDevice.getDevice(deviceId);
            gamepad_list.add(usbName(dev));
            gamepadId_list.add(deviceId);
        }
        usbHandler.obtainMessage(UsbDeviceHandler.MESSAGE_CONNECTED, deviceId).sendToTarget();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        int index = 0;
        int foundIndex = -1;
        for(String s:gamepad_list)
        {
            if ( s.startsWith(String.valueOf(deviceId)+":") )
            {
                foundIndex = index;
            }
            index++;
        }
        if ( foundIndex != -1 )
        {
            gamepad_list.remove(foundIndex);
            gamepadId_list.remove(foundIndex);

        }
        usbHandler.obtainMessage(UsbDeviceHandler.MESSAGE_DISCONNECTED, deviceId).sendToTarget();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {

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
