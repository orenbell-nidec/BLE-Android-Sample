package com.nidec.blesample;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    public static UUID MLDP_PRIVATE_SERVICE = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455");
    public static UUID MLDP_DATA_PRIVATE_CHAR = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3");
    public static UUID MLDP_CONTROL_PRIVATE_CHAR = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616");
    public static UUID MLDP_CHAR_CONFIG = convertFromInteger(0x2902);

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice motor;
    BluetoothGatt gatt;
    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattCharacteristic characteristic = gatt.getService(MLDP_PRIVATE_SERVICE).getCharacteristic(MLDP_DATA_PRIVATE_CHAR);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(MLDP_CHAR_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            gatt.writeDescriptor(descriptor);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.println(0, "BLE", characteristic.getValue().toString());
            // TODO: Will not print hex bytes, only array info
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BluetoothGattCharacteristic characteristic = gatt.getService(MLDP_PRIVATE_SERVICE).getCharacteristic(MLDP_DATA_PRIVATE_CHAR);

            characteristic.setValue(new byte[] {1, 1});
            gatt.writeCharacteristic(characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }
    };

    public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        bluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (device.getName() == "SelecTech") {
                    motor = device;
                }
            }
        });

        gatt = motor.connectGatt(this, true ,gattCallback);

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothAdapter.stopLeScan(new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.stopLeScan(new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        bluetoothAdapter.stopLeScan(new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            }
        });
    }
}
