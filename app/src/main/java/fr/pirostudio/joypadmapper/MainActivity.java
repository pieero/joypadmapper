package fr.pirostudio.joypadmapper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableField;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fr.pirostudio.joypadmapper.databinding.ActivityMainBinding;
import fr.pirostudio.joypadmapper.databinding.BluetoothComViewBinding;
import fr.pirostudio.joypadmapper.databinding.GamepadKeyLinkBinding;
import fr.pirostudio.joypadmapper.databinding.LinkKeycodeViewBinding;

public class MainActivity extends AppCompatActivity implements InputManager.InputDeviceListener, BluetoothProfile.ServiceListener {

    public final ObservableField<String> bluetoothStat = new ObservableField<>();
    public final ObservableField<String> usbStat = new ObservableField<>();
    public final ObservableField<String> usbLog = new ObservableField<>();
    public final ObservableField<String> usbLog2 = new ObservableField<>();
    public final ObservableArrayList<String> gamepad_list = new ObservableArrayList<>();
    public final ArrayList<Integer> gamepadId_list = new ArrayList<>();

    public List<InputDevice> m_connectedPads;
    public Map<String, GamePadMapper> m_mappedPads;

    protected static int REQUEST_ENABLE_BT;

    static {
        REQUEST_ENABLE_BT = 1;
    }

    class BtComContainer {
        public View view;
        public Map<Integer, View> linkViews;

        public BtComContainer(View view) {
            this.view = view;
            this.linkViews = new HashMap<>();
        }
    }

    protected Set<BluetoothDevice> m_bt_devices;
    protected Map<String, BluetoothDevice> m_mappedDevices;
    protected Map<String, BtConnectThread> m_mappedBtConnect;
    protected Map<String, String> m_JoyToDevice;
    protected Map<Integer, String> m_usbId_to_btAddress;
    protected Map<String, BtComContainer> m_btComContainers;

    ActivityMainBinding binding;

    BluetoothHandler btHandler = new BluetoothHandler(this);

    public MainActivity() {
        m_mappedDevices = new HashMap<>();
        m_mappedBtConnect = new HashMap<>();
        m_JoyToDevice = new HashMap<>();

        m_connectedPads = new ArrayList<>();
        m_mappedPads = new HashMap<>();
        m_usbId_to_btAddress = new HashMap<>();
        m_btComContainers = new HashMap<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            // ask for activation of bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        BuildView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                BuildView();
                // Do something with the contact here (bigger example below)
            }
        }
    }

    protected void BuildView() {
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        binding.setModel(this);

        TabHost host = (TabHost) findViewById(android.R.id.tabhost);
        host.setup();

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {

            m_bt_devices = adapter.getBondedDevices();
            for (BluetoothDevice device : m_bt_devices) {
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BtConnectThread btConnect = null;
                if (m_mappedBtConnect.containsKey(deviceHardwareAddress)) {
                    btConnect = m_mappedBtConnect.get(deviceHardwareAddress);
                } else {
                    btConnect = new BtConnectThread(device, MainActivity.this);
                    m_mappedBtConnect.put(deviceHardwareAddress, btConnect);
                }
                btConnect.setHandler(btHandler);

                if (m_mappedPads.containsKey(deviceHardwareAddress) == false) {
                    m_mappedPads.put(deviceHardwareAddress, new GamePadMapper());
                }

                TabHost.TabSpec spec = host.newTabSpec(device.getName());
                BluetoothComViewBinding btComBinding = DataBindingUtil.inflate( getLayoutInflater(),R.layout.bluetooth_com_view,null,true);
                btComBinding.setItem(btConnect);
                btComBinding.setModel(this);
                View bluetoothComView = btComBinding.getRoot();
                bluetoothComView.setTag(device.getAddress());
                m_btComContainers.put(device.getAddress(), new BtComContainer(bluetoothComView) );
                Button connectButton = bluetoothComView.findViewById(R.id.ConnectButton);
                connectButton.setTag(R.id.tag_btAddress, device.getAddress());
                connectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String btAddress = (String)v.getTag(R.id.tag_btAddress);
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

                GamePadMapper mapper = m_mappedPads.get(deviceHardwareAddress);
                for(Integer keyCode : mapper.keys() ) {
                    BtCommands command = mapper.getKeyCommand(keyCode);
                    addLinkToView(command, gamepadId_list.get(btConnect.getGamepadIndex()),keyCode, command.getPressValue(), command.getReleaseValue());
                }



                //thread.start();
                //thread.write(hello);
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

        if ( gamepad_list.isEmpty() ) {
            gamepad_list.add(this.getResources().getString(R.string.NoGamepad));
            gamepadId_list.add(-1);
            for (Integer usbId : usbDevices) {
                InputDevice dev = InputDevice.getDevice(usbId);
                gamepad_list.add(usbName(dev));
                gamepadId_list.add(usbId);
            }
        } else {

        }

        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        im.getInputDeviceIds();
        im.registerInputDeviceListener(this, null);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        im.getInputDeviceIds();
        im.unregisterInputDeviceListener(this);
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
                    gameControllerDeviceIds.add(deviceId);
                }
            }
        }
        return gameControllerDeviceIds;
    }

    KeyEventEx mDpad = new KeyEventEx();

    protected void addAxisLinkToView(int usbDevice, int axisCode, String pressVal, String releaseVal) {
        String btAddress = m_usbId_to_btAddress.get(usbDevice);
        GamePadMapper mapper = m_mappedPads.get(btAddress);
        String keyCodeName = KeyEventEx.axisCodeToString(axisCode);
        BtCommands command = new BtCommands(keyCodeName);
        mapper.addKeyMap(axisCode, command);
        addLinkToView(command, usbDevice, axisCode, "", "");

    }

    protected void addKeyLinkToView(int usbDevice, int keyCode, String pressVal, String releaseVal) {
        String btAddress = m_usbId_to_btAddress.get(usbDevice);
        GamePadMapper mapper = m_mappedPads.get(btAddress);
        BtCommands command = new BtCommands(KeyEventEx.keyCodeToString(keyCode));
        mapper.addKeyMap(keyCode, command);
        addLinkToView(command, usbDevice, keyCode, "", "");
    }

    protected void addLinkToView(BtCommands command, int usbDevice, int keyCode, String pressVal, String releaseVal) {
        String btAddress = m_usbId_to_btAddress.get(usbDevice);
        //GamePadMapper mapper = m_mappedPads.get(usbDevice);

        GamepadKeyLinkBinding gklBinding = GamepadKeyLinkBinding.inflate(getLayoutInflater());
        gklBinding.setCommand(command);
        View linkView = gklBinding.getRoot();
        Button keyButton = linkView.findViewById(R.id.button_key);
        keyButton.setTag(R.id.tag_usbDevice, usbDevice);
        keyButton.setTag(R.id.tag_keyCode, keyCode);
        Button linkButton = linkView.findViewById(R.id.button_link);
        linkButton.setTag(R.id.tag_btCommand, command);
        linkButton.setText(R.string.link);
        linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button)v;
                BtCommands command = (BtCommands)b.getTag(R.id.tag_btCommand);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.LinkKeyCode);
                LinkKeycodeViewBinding lkvBinding = LinkKeycodeViewBinding.inflate(getLayoutInflater());
                lkvBinding.setCommand(command);
                builder.setView(lkvBinding.getRoot());
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
            }
        });

        BtComContainer container = m_btComContainers.get(btAddress);
        View btComView = container.view;
        container.linkViews.put(keyCode, linkView);
        LinearLayout mapLayout = btComView.findViewById(R.id.map_layout);
        mapLayout.addView(linkView);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        boolean retVal = false;
        //int press = mDpad.getDirectionPressed(ev);
        //if ( press != -1 )
        if ( m_usbId_to_btAddress.containsKey(ev.getDeviceId()) ) {
            String btAddress = m_usbId_to_btAddress.get(ev.getDeviceId());
            String axisStr = "";

            if ( m_mappedPads.containsKey(btAddress) ) {
                GamePadMapper mapper = m_mappedPads.get(btAddress);
                Set<Integer> unHandledKeys = mapper.handleEvent(ev);
                for (Integer k : unHandledKeys) {
                    // Add to view and to map
                    addAxisLinkToView(ev.getDeviceId(), k, "", "");
                }
                retVal = true;
            }
            usbLog2.set(axisStr);
        } else {
            retVal = super.dispatchGenericMotionEvent(ev);
        }
        return retVal;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        boolean retVal = false;
        int keyCode = ev.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER) {
            if (ev.getAction() == KeyEvent.ACTION_DOWN) {
                usbLog.set(KeyEvent.keyCodeToString(keyCode));
                usbLog2.set("usb2bt" + (m_usbId_to_btAddress.containsKey(ev.getDeviceId()) ? "OK" : "KO") + "mapped");
            }
            if ( m_usbId_to_btAddress.containsKey(ev.getDeviceId()) ) {
                String btAddress = m_usbId_to_btAddress.get(ev.getDeviceId());
                String axisStr = "";

                if ( m_mappedPads.containsKey(btAddress) ) {
                    GamePadMapper mapper = m_mappedPads.get(btAddress);
                    if ( ev.getRepeatCount() == 0 ) {
                        if (mapper.handleEvent(ev) == false) {
                            // Add to view and to map
                            addKeyLinkToView(ev.getDeviceId(), ev.getKeyCode(), "", "");
                        }
                    }
                    retVal = true;
                }

            } else {
                retVal = super.dispatchKeyEvent(ev);
            }
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
        BuildView();
    }

    @Override
    public  void onServiceDisconnected(int profile) {

    }

}
