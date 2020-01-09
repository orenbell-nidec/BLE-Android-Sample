package com.nidec.blesample;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static BluetoothLeService mBluetoothLeService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // NOTE: Bind service in each activity's onStart
        Intent intent = new Intent(this, BluetoothLeService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // NOTE: Stop and unbind the BLE service when activity stops
        mBluetoothLeService.stopScan();
        this.unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // NOTE: Stop and unbind the BLE service when activity stops
        mBluetoothLeService.stopScan();
        this.unbindService(mServiceConnection);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // NOTE: Stop and unbind the BLE service when activity stops???
        mBluetoothLeService.stopScan();
        this.unbindService(mServiceConnection);
    }

    public void connectBT(View view) {
        // NOTE: Perform a scan and connect to the device when needed
        mBluetoothLeService.connect();
    }

    public void sendData(View view) {
        mBluetoothLeService.sendData(new byte[]{0,1,2,3});
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {		//Create new ServiceConnection interface to handle connection and disconnection

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {		//Service connects
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();	//Get a link to the service
            try
            {
                mBluetoothLeService.initialize();   //See if the service did not initialize properly
            } catch (RuntimeException err) {
                Toast.makeText(MainActivity.this, "Unable to initialize Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {			//Service disconnects
            mBluetoothLeService = null;								//Service has no connection
        }
    };
}
