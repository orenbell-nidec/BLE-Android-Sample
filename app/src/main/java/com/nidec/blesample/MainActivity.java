package com.nidec.blesample;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static BluetoothLeService mBluetoothLeService;
    TextView t_connected;
    TextView t_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        t_connected = findViewById(R.id.textView);
        t_data = findViewById(R.id.textView2);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // NOTE: Bind service in each activity's onStart
        Intent intent = new Intent(this, BluetoothLeService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, bleServiceIntentFilter());
    }

        @Override
    protected void onStop() {
        super.onStop();

        // NOTE: Stop and unbind the BLE service when activity stops
        if (mBluetoothLeService != null) {
            mBluetoothLeService.stopScan();

            this.unbindService(mServiceConnection);
        }
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

        unregisterReceiver(gattUpdateReceiver);
    }

    public void performScan(View view) {
        // NOTE: Perform a scan and startScan to the device when needed
        if (!mBluetoothLeService.startScan()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BluetoothLeService.REQUEST_ENABLE_BT);
        } else {
            t_connected.setText("Scanning...");
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {		//Create new ServiceConnection interface to handle connection and disconnection

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {		//Service connects
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();	//Get a link to the service
            if (!mBluetoothLeService.initialize()) {   //See if the service did not initialize properly
                Toast.makeText(MainActivity.this, "Unable to initialize Bluetooth", Toast.LENGTH_SHORT).show();
            }


        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {			//Service disconnects
            mBluetoothLeService = null;								//Service has no connection
        }
    };

    public void sendData(View view) {
        EditText editText = findViewById(R.id.editText);
        String[] hexstring = editText.getText().toString().split(" ");

        byte[] data = new byte[hexstring.length];
        int i = 0;
        for (String s : hexstring) {
            data[i++] = (byte) ((Character.digit(s.charAt(0), 16) << 4)
                    + Character.digit(s.charAt(1), 16));
        }

        mBluetoothLeService.sendData(data);
    }

    private static IntentFilter bleServiceIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_REQ_ENABLE_BT);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_DATA_RECEIVED);
        return intentFilter;
    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_BLE_DATA_RECEIVED.equals(action)) {
                char[] data = BluetoothLeService.bytesToHexString(intent.getByteArrayExtra(BluetoothLeService.INTENT_EXTRA_SERVICE_DATA));

                t_data.setText(data, 0, data.length);
            }
        }
    };
}
