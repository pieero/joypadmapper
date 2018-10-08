package fr.pirostudio.joypadmapper;

import java.util.Set;

public class PadEvent extends Object {
    public enum Type {
        TYPE_KEY,
        TYPE_AXIS_KEY,
        TYPE_AXIS,
        TYPE_COMBO
    }

    public Type type;
    public Set<Integer> combo_keys;

    public boolean hasKey(int keyCode) {
        return combo_keys.contains(keyCode);
    }

    @Override
    public boolean equals(Object keyCode) {
        if ( keyCode instanceof PadEvent )
        {
            return ((PadEvent)keyCode).combo_keys.equals(this.combo_keys) && ((PadEvent)keyCode).type == this.type;
        }
        else if ( keyCode instanceof Integer )
        {
            return ( this.type == Type.TYPE_AXIS_KEY || this.type == Type.TYPE_KEY ) && this.combo_keys.contains((Integer)keyCode);
        }
        return false;
    }
}
