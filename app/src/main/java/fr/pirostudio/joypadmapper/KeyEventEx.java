package fr.pirostudio.joypadmapper;

import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.HashSet;
import java.util.Set;

public class KeyEventEx {

    static Set<Integer> getAxisInMotion(MotionEvent motionEvent) {
        Set<Integer> retVal = new HashSet<>();
        for(int axis = MotionEvent.AXIS_X; axis < MotionEvent.AXIS_GENERIC_16+1; ++axis) {

            try {
                float axisVal = motionEvent.getAxisValue(axis);
                if (Math.abs(axisVal) > 0.1) {
                    retVal.add(axis);
                }
            } catch (Exception e) {
            }
        }
        return retVal;
    }

    static public Set<Integer> buildKeyCodeFromAxis(MotionEvent ev) {
        Set<Integer> retVal = new HashSet<>();
        Set<Integer> axis = getAxisInMotion(ev);
        for(Integer x : axis )
        {
            if ( ev.getAxisValue(x) >= (float)1. )
            {
                retVal.add(10*x + 1);
            }
            if ( ev.getAxisValue(x) <= (float)-1. )
            {
                retVal.add(10*x + 2);
            }
        }
        return retVal;
    }

    public static boolean isDpadDevice(InputEvent event) {
        // Check that input comes from a device with directional pads.
        if ((event.getSource() & InputDevice.SOURCE_DPAD) != InputDevice.SOURCE_DPAD) {
            return true;
        } else {
            return false;
        }
    }

    public static String axisCodeToString(int key)
    {
        String retVal = "undefined";
        int axis = key / 10;
        int direction = key % 10;
        String directionStr = "+";
        if ( direction == 2 )
            directionStr = "-";
        retVal = directionStr + MotionEvent.axisToString(axis).substring("AXIS_".length());
        return retVal;
    }
    public static String keyCodeToString(int key)
    {
        return KeyEvent.keyCodeToString(key).substring("KEYCODE_".length());
    }


}
