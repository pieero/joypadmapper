package fr.pirostudio.joypadmapper;

import android.view.InputDevice;

import java.util.HashMap;

public class GamePadMapper  {

    private InputDevice m_Device;
    private BtConnectThread m_btConnect;
    private HashMap<Integer, BtCommands> m_keyMap;
    private HashMap<Integer, BtCommands> m_axisMap;

    public GamePadMapper(InputDevice p_device) {
        m_Device = p_device;
        m_keyMap = new HashMap<>();
        m_axisMap = new HashMap<>();
        m_btConnect = null;
    }

    void setBtConnect(BtConnectThread p_btConnect) {
        m_btConnect = p_btConnect;
    }

    void addKeyMap(int key, BtCommands command)
    {
        m_keyMap.put(key,command);
    }

    public BtCommands getKeyCommand(int keyCode) {
        return m_keyMap.get(keyCode);
    }

    void addAxisMap(int axis, BtCommands command)
    {
        m_axisMap.put(axis,command);
    }

    void clearMap()
    {
        m_keyMap.clear();
    }

    public boolean handleKeyCode(int keyCode) {
        return m_keyMap.containsKey( keyCode );
    }
    public boolean handleAxisCode(int keyCode) {
        return m_axisMap.containsKey( keyCode );
    }

    public boolean onKeyDown(int keyCode) {
        if ( m_keyMap.containsKey( keyCode ) )
        {
            if ( m_btConnect != null && m_btConnect.isConnected()) {
                BtCommands btCom = m_keyMap.get(keyCode);
                m_btConnect.write(btCom.getPressValue().getBytes());
                return true;
            }
        }
        return false;
    }

    public boolean onAxisKeyDown(int keyCode) {
        if ( m_axisMap.containsKey( keyCode ) )
        {
            if ( m_btConnect != null && m_btConnect.isConnected()) {
                BtCommands btCom = m_axisMap.get(keyCode);
                m_btConnect.write(btCom.getPressValue().getBytes());
                return true;
            }
        }
        return false;
    }


    public boolean onKeyUp(int keyCode) {
        if ( m_keyMap.containsKey( keyCode ) )
        {
            if ( m_btConnect != null && m_btConnect.isConnected() ) {
                BtCommands btCom = m_keyMap.get(keyCode);
                m_btConnect.write(btCom.getReleaseValue().getBytes());
                return true;
            }
        }
        return false;
    }

}
