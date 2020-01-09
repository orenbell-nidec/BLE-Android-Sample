package com.nidec.blesample;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private final static int REQUEST_ENABLE_BT = 1;
    public static UUID MLDP_PRIVATE_SERVICE = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455");
    public static UUID MLDP_DATA_PRIVATE_CHAR = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3");
    public static UUID MLDP_CONTROL_PRIVATE_CHAR = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616");
    public static UUID MLDP_CHAR_CONFIG = convertFromInteger(0x2902);

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private Handler mHandler;
    public BluetoothDevice motor;
    private BluetoothGatt gatt;
    private BluetoothLeScanner leScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    public final long SCAN_PERIOD = 10000;
    private final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            }

            // TODO: More error handling here
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

            // TODO: Broadcast an update
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            // TODO:
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
            byte[] bytes = characteristic.getValue();
            char[] hexStr = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xFF;
                hexStr[i * 2] = HEX_ARRAY[v >>> 4];
                hexStr[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }

            Log.println(Log.INFO, "BLE", hexStr.toString());
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

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();

            // Connect to device, if it's what we want
            tryConnectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            Log.d(TAG, "LocalBinder.getService Start & End");
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind Start & End");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind Start & End");
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

    public boolean initialize() {
        mHandler = new Handler();
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            throw new RuntimeException("Unable to declare a bluetooth adapter");
        }

        return true;
    }

    public void connect() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            leScanner = bluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
//                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setReportDelay(0)
                    .build();

            scanLeDevice(true);
        }
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    leScanner.stopScan(scanCallback);
                }
            }, SCAN_PERIOD);
            leScanner.startScan(filters, settings, scanCallback);
        } else {
            leScanner.stopScan(scanCallback);
        }
    }

    public void stopScan() {
        leScanner.stopScan(scanCallback);
    }

    private void tryConnectToDevice(BluetoothDevice device) {
        if (gatt == null && device.getName() != null && device.getName().contains("SelecTech")) {
            gatt = device.connectGatt(this, true, gattCallback);
            scanLeDevice(false);
        }
    }

    public void sendData(byte[] data) {
        BluetoothGattService service = gatt.getService(MLDP_PRIVATE_SERVICE);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(MLDP_DATA_PRIVATE_CHAR);
        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        descriptors.get(0).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gatt.writeDescriptor(descriptors.get(0));
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        Log.d(TAG, "close Start & End");
        if (gatt == null) {
            return;
        }
        gatt.close();
        gatt = null;
    }
}
