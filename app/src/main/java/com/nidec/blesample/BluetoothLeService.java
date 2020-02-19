package com.nidec.blesample;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();
    public final static int REQUEST_ENABLE_BT = 1;
    public static UUID MLDP_PRIVATE_SERVICE = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455");
    public static UUID MLDP_WRITE_CHAR = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3");
    public static UUID MLDP_READ_CHAR = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616");
    public static UUID MLDP_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public int connectionState;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private Handler mHandler;
    public BluetoothDevice motor;
    private BluetoothGatt gatt;
    private BluetoothLeScanner leScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    public final long SCAN_PERIOD = 10000;
    private final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private boolean mScanning = false;

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

    public static char[] bytesToHexString(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexStr = new char[bytes.length * 3];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexStr[i * 3] = HEX_ARRAY[v >>> 4];
            hexStr[i * 3 + 1] = HEX_ARRAY[v & 0x0F];
            hexStr[i * 3 + 2] = ' ';
        }

        return hexStr;
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.i(TAG, "onConnectionStateChange GATT FAILURE");
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onConnectionStateChange != GATT_SUCCESS");
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChange CONNECTED");
                connectionState = BluetoothProfile.STATE_CONNECTED;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange DISCONNECTED");
                connectionState = BluetoothProfile.STATE_DISCONNECTED;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.i(TAG, "onConnectionStateChange DISCONNECTING");
                connectionState = BluetoothProfile.STATE_DISCONNECTING;
            }

            // TODO: More error handling here
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            // If unsuccessful, ignore this
            if (status != BluetoothGatt.GATT_SUCCESS) return;

            // Set up all UUIDs
            BluetoothGattCharacteristic characteristic_data = gatt.getService(MLDP_PRIVATE_SERVICE).getCharacteristic(MLDP_WRITE_CHAR);
            gatt.setCharacteristicNotification(characteristic_data, true);

            BluetoothGattCharacteristic characteristic_control = gatt.getService(MLDP_PRIVATE_SERVICE).getCharacteristic(MLDP_READ_CHAR);
            gatt.setCharacteristicNotification(characteristic_control, true);
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
            super.onCharacteristicChanged(gatt, characteristic);

            Log.d(TAG, "Value is: " + bytesToHexString(characteristic.getValue()));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BluetoothGattCharacteristic characteristic = gatt.getService(MLDP_PRIVATE_SERVICE).getCharacteristic(MLDP_WRITE_CHAR);

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

            // Connect to device, and remember it for later
            if (btDevice.getName() != null && btDevice.getName().contains("SelecTech")) {
                scanLeDevice(false);
                bluetoothDevice = btDevice;
                connect();  // TODO: See if this can be done at a different time
            }
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

    public boolean initialize() {
        mHandler = new Handler();
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Unable to declare a bluetooth adapter");
            return false;
        }

        return true;
    }

    public boolean startScan() {
        // TODO: Request user enable BT (REQUEST_ENABLE_BT intent) if bt is disabled
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            leScanner = bluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setReportDelay(0)
                    .build();

            scanLeDevice(true);
            return true;
        } else return false;
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    if (bluetoothAdapter.isEnabled()) {
                        leScanner.stopScan(scanCallback);
                    }
                }
            }, SCAN_PERIOD);
            mScanning = true;
            leScanner.startScan(filters, settings, scanCallback);
        } else {
            mScanning = false;
            if (bluetoothAdapter.isEnabled()) {
                leScanner.stopScan(scanCallback);
            }
        }
    }

    public void stopScan() {
        mScanning = false;
        if (bluetoothAdapter.isEnabled()) {
            leScanner.stopScan(scanCallback);
        }
    }

    public boolean connect() {
        if (bluetoothAdapter == null || bluetoothDevice == null) {
            Log.w(TAG, "Bluetooth adapter not initialized or scan failed");
            return false;
        }

        // Update the state
        gattCallback.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTING);

        // If we've already initialized gatt, just reconnect
        if (gatt != null) {
            // Connect the device
            bluetoothDevice.connectGatt(BluetoothLeService.this, false, gattCallback);

            // Connect gatt
            if (gatt.connect()) {
                connectionState = BluetoothProfile.STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        gatt = bluetoothDevice.connectGatt(this,true, gattCallback, BluetoothDevice.TRANSPORT_LE);

        connectionState = BluetoothProfile.STATE_CONNECTING;
        return true;
    }

    public String getDeviceAddress() {
        if (bluetoothDevice == null) {
            return null;
        } else {
            return bluetoothDevice.getAddress();
        }
    }

    public void sendData(byte[] data) {
        try {
            BluetoothGattService service = gatt.getService(MLDP_PRIVATE_SERVICE);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(MLDP_READ_CHAR);
            characteristic.setValue(data);
            gatt.writeCharacteristic(characteristic);
        } catch (Exception err) {
            Log.e(TAG, err.getStackTrace().toString());
        }

    }

    public byte[] readData() {
        if (bluetoothAdapter == null || gatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }

        BluetoothGattService service = gatt.getService(MLDP_PRIVATE_SERVICE);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(MLDP_READ_CHAR);
        gatt.readCharacteristic(characteristic);

        return characteristic.getValue();
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
