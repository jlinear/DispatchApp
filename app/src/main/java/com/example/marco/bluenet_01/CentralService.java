package com.example.marco.bluenet_01;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

/**
 * Created by jerry on 4/19/18.
 */

public class CentralService extends Service{
    /**** **** debug tag declaration **** ****/
    private static final String ERR_TAG = "PRIMARY ERROR";
    private static final String INFO_TAG = "APP_INFO";
    private static final String DEBUG_TAG = "DEBUG_STATUS";

    /**** **** end of tag declaration **** ****/

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    protected static final UUID MSG_SERVICE_UUID = UUID
            .fromString("00001869-0000-1000-8000-00805f9b34fb");
    private static final UUID MSG_CHAR_UUID = UUID.
            fromString("00002a09-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHAR_CONFI_UUID = UUID.
            fromString("00002a08-0000-1000-8000-00805f9b34fb");

    /**** **** Service Binder **** ****/
    //If called as a service, it needs to switch between multiple same service instances when maintaining multiple connections.
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        CentralService getService(){
            return CentralService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //close();
        return super.onUnbind(intent);
    }
    /**** **** end of Service Binder **** ****/

    /**** **** Variable Declaration **** ****/
    //ADSCActivity adsc = new ADSCActivity();
    public BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public List<BluetoothGattService> mBluetoothGattService;
    /**** **** end of Variable Declaration **** ****/

    //Gatt/
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate("connected");
                Log.d(DEBUG_TAG, "connected to " + gatt.getDevice().getAddress());
                // Attempts to discover services after successful connection.
                Log.d(DEBUG_TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.d(DEBUG_TAG, "disconnected: " + gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS) {
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.i(INFO_TAG,"GATT service discovered!");
                mBluetoothGattService = gatt.getServices();
//                if(gatt.getService(MSG_SERVICE_UUID) != null){
//                    gatt.setCharacteristicNotification(gatt.getService(MSG_SERVICE_UUID).getCharacteristic(MSG_CHAR_UUID),true);
//                }
                Log.d(DEBUG_TAG,mBluetoothGattService.get(2).getUuid().toString());
            } else {
                Log.e(ERR_TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == GATT_SUCCESS) {
                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate("char_changed", characteristic);
            Log.d("CENTRAL","char changed");
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            if(status == GATT_SUCCESS){
                Log.d("CENTRAL", "write to P");
            }

        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            String data_out = new String(data);
            intent.putExtra("msg",data_out);
//            for(byte byteChar : data)
//                stringBuilder.append(String.format("%02X ", byteChar));
//            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }
        sendBroadcast(intent);
    }


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(ERR_TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(ERR_TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }


    public boolean connect(final BluetoothDevice mDevice) {
//        if (mBluetoothAdapter == null || address == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
//            return false;
//        }
//
        // Previously connected device.  Try to reconnect.
        if (mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
//
//        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//        if (device == null) {
//            Log.w(TAG, "Device not found.  Unable to connect.");
//            return false;
//        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        //BluetoothDevice cdevice = mBluetoothAdapter.getRemoteDevice(mDevice.getAddress());
        mBluetoothGatt = mDevice.connectGatt(this, false, mGattCallback);
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void writeCharacteristic(BluetoothGatt gatt, String msg){
        BluetoothGattCharacteristic mchar = gatt.getService(MSG_SERVICE_UUID).getCharacteristic(MSG_CHAR_UUID);
        mchar.setValue(msg);
        gatt.writeCharacteristic(mchar);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

//        // This is specific to chat.
//        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//                    UUID.fromString(ADSCActivity.CLIENT_CHARACTERISTIC_CONFIG));
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);
//        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    public BluetoothGattService getGattService(UUID uuid){
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getService(uuid);
    }



}

