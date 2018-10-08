package fr.pirostudio.joypadmapper;

import android.content.Context;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GamePadMapper implements Serializable {

    private BtConnectThread m_btConnect;
    private Map<Integer, BtCommands> m_keyMap;
    private Set<Integer> m_currentAxis;

    private static java.lang.reflect.Type mapType = new TypeToken<Map<Integer, BtCommands>>() {}.getType();

    public void saveToFile(FileOutputStream fOut) {
        try {
            fOut.write(toJson().getBytes());
        } catch(Exception e) {

        }
    }

    public void loadFromFile(FileInputStream fIn) {
        try {
            int c;
            String temp="";
            while( (c = fIn.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
            loadJson(temp);
        } catch(Exception e) {

        }
    }

    public String toJson() {
        Gson g = new Gson();
        String retVal = "";
        retVal += g.toJson(m_keyMap, mapType);
        return retVal;
    }

    public void loadJson(String json) {
        Gson g = new Gson();
        m_keyMap = g.fromJson(json, mapType);
    }

    //history
    private List<PadEvent> padHistory;

    public GamePadMapper() {
        m_keyMap = new HashMap<>();
        m_currentAxis = new HashSet<>();
        m_btConnect = null;
    }

    public Set<Integer> keys() {
        return m_keyMap.keySet();
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

    void clearMap()
    {
        m_keyMap.clear();
    }

    public boolean handleKeyCode(int keyCode) {
        return m_keyMap.containsKey( keyCode );
    }

    public Set<Integer> handleEvent(MotionEvent ev) {
        Set<Integer> retVal = new HashSet<>();
        Set<Integer> currentKeys = KeyEventEx.buildKeyCodeFromAxis(ev);
        Set<Integer> oldKeys = new HashSet<>(m_currentAxis);
        Set<Integer> newKeys = new HashSet<>();
        for (Integer key : currentKeys) {
            if (oldKeys.contains(key)) {
                oldKeys.remove(key);
            } else {
                newKeys.add(key);
            }
        }
        for (Integer oldKey : oldKeys) {
            onKeyUp(oldKey);
        }
        for (Integer newKey : newKeys) {
            if ( m_keyMap.containsKey(newKey) ) {
                onKeyDown(newKey);
            }
            else {
                retVal.add(newKey);
            }
        }
        m_currentAxis = currentKeys;
        return retVal;
    }

    public boolean handleEvent(KeyEvent ev) {
        boolean retVal = false;
        if ( m_keyMap.containsKey(ev.getKeyCode()) ) {
            if (ev.getAction() == KeyEvent.ACTION_UP) {
                onKeyUp(ev.getKeyCode());
            } else if (ev.getAction() == KeyEvent.ACTION_DOWN) {
                onKeyDown(ev.getKeyCode());
            }
            retVal = true;
        }
        return retVal;
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
