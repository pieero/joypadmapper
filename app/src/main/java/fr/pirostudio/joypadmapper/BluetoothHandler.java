package fr.pirostudio.joypadmapper;

import android.os.Handler;
import android.os.Message;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

class UsbLinkedObject {
    public final int usbIndex;
    public final String btAddress;

    public UsbLinkedObject(int usbIndex, String btAddress) {
        this.usbIndex = usbIndex;
        this.btAddress = btAddress;
    }
}

class BluetoothHandler extends Handler {

    public static final int MESSAGE_CONNECTED = 0;
    public static final int MESSAGE_DISCONNECTED = 1;
    public static final int MESSAGE_USB_LINKED = 2;

    private MainActivity m_main;

    public BluetoothHandler(MainActivity main) {
        super();
        this.m_main = main;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case MESSAGE_CONNECTED:
                break;
            case MESSAGE_DISCONNECTED:
                break;
            case MESSAGE_USB_LINKED:
                UsbLinkedObject link = (UsbLinkedObject)msg.obj;
                if( link.usbIndex == 0 ) {
                    Iterator it = m_main.m_usbId_to_btAddress.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        if ( pair.getValue() == link.btAddress ) {
                            m_main.usbLog2.set(String.format(Locale.ENGLISH, "unmap %d(%d) -> %s",pair.getKey(), link.usbIndex, link.btAddress));
                            Integer usbDeviceId = (Integer)pair.getKey();
                            if (m_main.m_mappedPads.containsKey(usbDeviceId)) {
                                m_main.m_mappedPads.get(usbDeviceId).setBtConnect(null);
                            }
                            else
                            {
                                m_main.usbLog2.set("unmap: no usbDev "+ String.valueOf(usbDeviceId) +" in m_mappedPads");
                            }
                            it.remove(); // avoids a ConcurrentModificationException
                            break;
                        }
                    }
                }else{
                    if( m_main.gamepadId_list.size() > link.usbIndex ) {
                        int usbDeviceId = m_main.gamepadId_list.get(link.usbIndex);
                        m_main.m_usbId_to_btAddress.put(usbDeviceId, link.btAddress);
                        if (m_main.m_mappedPads.containsKey(usbDeviceId) && m_main.m_mappedBtConnect.containsKey(link.btAddress)) {
                            m_main.m_mappedPads.get(usbDeviceId).setBtConnect(m_main.m_mappedBtConnect.get(link.btAddress));
                        }
                        else
                        {
                            m_main.usbLog2.set("map: no usbDev "+ String.valueOf(usbDeviceId) +" in m_mappedPads OR '"+link.btAddress+"' in m_mappedBtConnect");
                        }
                        m_main.usbLog2.set(String.format(Locale.ENGLISH, "map %d(%d) -> %s", m_main.gamepadId_list.get(link.usbIndex), link.usbIndex, link.btAddress));
                    }
                    else
                    {
                        m_main.usbLog2.set("map: no index "+ String.valueOf(link.usbIndex) +" in gamepadId_list");
                    }
                }
                break;
        }
        //switch (msg.what) {
    }
}