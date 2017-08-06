package com.example.caseycarroll.ledspeedometer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BLEService extends Service {
    public BLEService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
