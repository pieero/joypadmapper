package fr.pirostudio.joypadmapper;

import android.os.Vibrator;
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
        m_btConnect = null;
    }

    void setBtConnect(BtConnectThread p_btConnect) {
        m_btConnect = p_btConnect;
    }

    void addKeyMap(int key, BtCommands command)
    {
        m_keyMap.put(key,command);
    }

    void addAxisMap(int axis, BtCommands command)
    {
        m_axisMap.put(axis,command);
    }

    void clearMap()
    {
        m_keyMap.clear();
    }

    public boolean onKeyDown(int keyCode) {
        if ( m_keyMap.containsKey( keyCode ) )
        {
            BtCommands btCom = m_keyMap.get(keyCode);
            m_btConnect.write(btCom.pressValue.getBytes());
            return true;
        }
        else
        {
            if ( m_Device.getVibrator() != null && m_Device.getVibrator().hasVibrator() )
            {
                Vibrator v = m_Device.getVibrator();
                v.vibrate(200);
            }
        }
        return false;
    }

    public boolean onKeyUp(int keyCode) {
        if ( m_keyMap.containsKey( keyCode ) )
        {
            BtCommands btCom = m_keyMap.get(keyCode);
            m_btConnect.write(btCom.releaseValue.getBytes());
            return true;
        }
        return false;
    }

}
