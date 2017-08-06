package com.example.caseycarroll.ledspeedometer;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableField;
import android.databinding.ObservableFloat;

/**
 * A class that uses Observables to be easily updated via the android databinding library
 */

public class User extends BaseObservable {
    public final ObservableFloat userSpeed = new ObservableFloat();
    public final ObservableField<String> location = new ObservableField<>();
}
