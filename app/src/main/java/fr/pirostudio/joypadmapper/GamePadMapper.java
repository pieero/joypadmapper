package fr.pirostudio.joypadmapper;

import android.os.Vibrator;
import android.view.InputDevice;
import android.view.KeyEvent;

import java.util.HashMap;

public class GamePadMapper implements KeyEvent.Callback {

    private InputDevice m_Device;
    private BtConnectThread m_btConnect;
    private HashMap<Integer, BtCommands> m_map;

    public GamePadMapper(InputDevice p_device) {
        m_Device = p_device;
        m_map = new HashMap<>();
        m_btConnect = null;
    }

    void setBtConnect(BtConnectThread p_btConnect) {
        m_btConnect = p_btConnect;
    }

    void addMap(int key, BtCommands command)
    {
        m_map.put(key,command);
    }

    void clearMap()
    {
        m_map.clear();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( m_map.containsKey( event.getKeyCode() ) )
        {
            BtCommands btCom = m_map.get(event.getKeyCode());
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

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ( m_map.containsKey( event.getKeyCode() ) )
        {
            BtCommands btCom = m_map.get(event.getKeyCode());
            m_btConnect.write(btCom.releaseValue.getBytes());
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return false;
    }
}
