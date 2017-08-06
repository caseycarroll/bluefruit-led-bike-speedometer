package com.example.caseycarroll.ledspeedometer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.caseycarroll.ledspeedometer.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_FINE_LOCATION = 123;
    private final int REQUEST_ENABLE_BT = 132;
    private final int LOCATION_UPDATE_INTERVAL = 50;
    private final int LOCATION_UPDATE_INTERVAL_FASTEST = 100;
    private String TAG = "MainActivity";
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mRequestingLocationUpdates;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private User mUser;

    private Location oldLocation;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLeBluetootchScanner;
    private ScanCallback mScanCallback;
    private Handler mHandler;
    private BluetoothDevice BlueFruit;

    private Button connectButton;
    private BluetoothGatt mBluetoothGatt;
    private android.bluetooth.BluetoothGattCallback mGattCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //inflate view with binding library
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        //hook up to button
        connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: attempting to connect to ble device");
                connectToBlueFruit();
            }
        });
        //hook up binding to User obj
        mUser = new User();
        binding.setUser(mUser);
        mHandler = new Handler();

        //check if necessary permissions are enabled
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_FINE_LOCATION);
        } else {
            mRequestingLocationUpdates = true;
        }

        //create instance of fused location provider
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //set up a location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                    //after every successful result, update speed value on UI
                    Location location = locationResult.getLastLocation();
                    mUser.userSpeed.set(location.getSpeed());
            }
        };

        //create a location request object
        createLocationRequest();

        //Initialize Bluetooth adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //TODO: try contextcompat
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // callback that looks for bluefruit device
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if(result.getDevice().getName() != null
                        && result.getDevice().getName().equalsIgnoreCase("bluefruit52")) {
                    Log.d(TAG, "onScanResult: recognized bluefruit");
                    BlueFruit = result.getDevice();
                    mLeBluetootchScanner.stopScan(mScanCallback);
                    connectButton.setEnabled(true);
                }
            }
        };

        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "onServicesDiscovered: Communication is ready");
                    for (BluetoothGattService service: gatt.getServices()) {
                        Log.d(TAG, "onServicesDiscovered: Service: " + service);
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d(TAG, "onCharacteristicWrite: ");
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        Log.d(TAG, "onConnectionStateChange: Connection made with Bluefruit");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectButton.setText(R.string.connected);
                                connectButton.setEnabled(false);
                            }
                        });

                        mBluetoothGatt.discoverServices();

                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        Log.d(TAG, "onConnectionStateChange: Disconnected with Bluefruit");
                        mBluetoothGatt.close();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectButton.setText(R.string.connect);
                                connectButton.setEnabled(true);
                            }
                        });
                        break;
                    default:
                        Log.d(TAG, "onConnectionStateChange: Status wasn't detected...");
                }
            }
        };
    }

    private void connectToBlueFruit() {
        mBluetoothGatt = BlueFruit.connectGatt(this, false, mGattCallback);
    }

    private void startLeScanTask() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLeBluetootchScanner.stopScan(mScanCallback);
            }
        }, 20000);

        mLeBluetootchScanner.startScan(mScanCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_FINE_LOCATION: {
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted
                    Log.d(TAG, "onRequestPermissionsResult: Permission granted by user");
                    mRequestingLocationUpdates = true;
                } else {
                    mRequestingLocationUpdates = false;
                }
            }
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_UPDATE_INTERVAL_FASTEST);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Ensure Bluetooth is not available or disabled.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        mLeBluetootchScanner = mBluetoothAdapter.getBluetoothLeScanner();
        startLeScanTask();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        disconnectFromBluefruit();
    }

    private void disconnectFromBluefruit() {
        if(mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
}