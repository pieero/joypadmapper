package fr.pirostudio.joypadmapper;

import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class KeyEventEx {

    public final static int KEYCODE_DPAD_UP_LEFT = 1000;
    public final static int KEYCODE_DPAD_UP_RIGHT = 1001;
    public final static int KEYCODE_DPAD_DOWN_RIGHT = 1002;
    public final static int KEYCODE_DPAD_DOWN_LEFT = 1003;

    public int getDirectionPressed(InputEvent event) {
        if (!isDpadDevice(event)) {
            return -1;
        }

        int directionPressed = -1;

        // If the input event is a MotionEvent, check its hat axis values.
        if (event instanceof MotionEvent) {

            // Use the hat axis value to find the D-pad direction
            MotionEvent motionEvent = (MotionEvent) event;
            float xaxis = motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X);
            float yaxis = motionEvent.getAxisValue(MotionEvent.AXIS_HAT_Y);

            // Check if the AXIS_HAT_X value is -1 or 1, and set the D-pad
            // LEFT and RIGHT direction accordingly.
            if (Float.compare(xaxis, -1.0f) == 0) {
                directionPressed =  KeyEvent.KEYCODE_DPAD_LEFT;
            } else if (Float.compare(xaxis, 1.0f) == 0) {
                directionPressed =  KeyEvent.KEYCODE_DPAD_RIGHT;
            }
            // Check if the AXIS_HAT_Y value is -1 or 1, and set the D-pad
            // UP and DOWN direction accordingly.
            if (Float.compare(yaxis, -1.0f) == 0) {
                switch(directionPressed)
                {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        directionPressed =  KeyEventEx.KEYCODE_DPAD_UP_LEFT;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        directionPressed =  KeyEventEx.KEYCODE_DPAD_UP_RIGHT;
                        break;
                    default:
                        directionPressed =  KeyEvent.KEYCODE_DPAD_UP;
                }
            } else if (Float.compare(yaxis, 1.0f) == 0) {
                switch(directionPressed)
                {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        directionPressed =  KeyEventEx.KEYCODE_DPAD_DOWN_LEFT;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        directionPressed =  KeyEventEx.KEYCODE_DPAD_DOWN_RIGHT;
                        break;
                    default:
                        directionPressed =  KeyEvent.KEYCODE_DPAD_DOWN;
                }
            }
        }

        // If the input event is a KeyEvent, check its key code.
        else if (event instanceof KeyEvent) {

            // Use the key code to find the D-pad direction.
            KeyEvent keyEvent = (KeyEvent) event;
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT ||
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP ||
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER ) {
                directionPressed = keyEvent.getKeyCode();
            }
        }
        return directionPressed;
    }

    public static boolean isDpadDevice(InputEvent event) {
        // Check that input comes from a device with directional pads.
        if ((event.getSource() & InputDevice.SOURCE_DPAD) != InputDevice.SOURCE_DPAD) {
            return true;
        } else {
            return false;
        }
    }

    public static String keyCodeToString(int key)
    {
        String retVal = "undefined";
        switch(key)
        {
            case KEYCODE_DPAD_DOWN_LEFT:
                retVal = "KEYCODE_DPAD_DOWN_LEFT";
                break;
            case KEYCODE_DPAD_DOWN_RIGHT:
                retVal = "KEYCODE_DPAD_DOWN_RIGHT";
                break;
            case KEYCODE_DPAD_UP_LEFT:
                retVal = "KEYCODE_DPAD_UP_LEFT";
                break;
            case KEYCODE_DPAD_UP_RIGHT:
                retVal = "KEYCODE_DPAD_UP_RIGHT";
                break;
            default:
                retVal = KeyEvent.keyCodeToString(key);
                        break;
        }
        return retVal;
    }
}
