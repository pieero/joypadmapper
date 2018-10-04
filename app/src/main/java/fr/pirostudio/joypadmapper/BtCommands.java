package fr.pirostudio.joypadmapper;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableField;
import android.databinding.ObservableFloat;

public class BtCommands extends BaseObservable {
    public final ObservableFloat threshold = new ObservableFloat();
    public final ObservableField<String> keyName = new ObservableField<>();
    private String pressValue;
    private String releaseValue;

    @Bindable
    public void setPressValue(String val) {
        if ( this.pressValue != val ) {
            this.pressValue = val;
            notifyChange();
        }
    }

    @Bindable
    public String getPressValue() {
        return this.pressValue;
    }

    @Bindable
    public void setReleaseValue(String val) {
        if ( this.releaseValue != val ) {
            this.releaseValue = val;
            notifyChange();
        }
    }

    @Bindable
    public String getReleaseValue() {
        return this.releaseValue;
    }

    public BtCommands(String name) {
        keyName.set(name);
        threshold.set((float)0.);
    }
}
