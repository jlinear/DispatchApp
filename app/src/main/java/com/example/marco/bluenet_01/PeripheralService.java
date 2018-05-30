package com.example.marco.bluenet_01;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.UUID;

/**
 * Created by jerry on 4/20/18.
 */

public class PeripheralService extends Service {
    /**** **** debug tag declaration **** ****/
    private static final String ERR_TAG = "PRIMARY ERROR";
    private static final String INFO_TAG = "APP_INFO";
    private static final String DEBUG_TAG = "DEBUG_STATUS";
    /**** **** end of tag declaration **** ****/

    /**** **** Service Binder **** ****/
    //If called as a service, it needs to switch between multiple same service instances when maintaining multiple connections.
    private final IBinder mBinder = new PeripheralService.LocalBinder();

    public class LocalBinder extends Binder {
        PeripheralService getService(){
            return PeripheralService.this;
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
    private BluetoothGattService mMsgService;
    private BluetoothGattCharacteristic mMsgCharacteristic;

    private static final UUID MSG_SERVICE_UUID = UUID
            .fromString("00001869-0000-1000-8000-00805f9b34fb");
    private static final UUID MSG_CHAR_UUID = UUID
            .fromString("00002909-0000-1000-8000-00805f9b34fb");

    /**** **** end of Variable Declaration **** ****/

    private BluetoothGattServer mGattServer;
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS){
                if (newState == BluetoothGatt.STATE_CONNECTED){

                }
            }
        }
    };

    public PeripheralService(){
        mMsgCharacteristic = new BluetoothGattCharacteristic(MSG_SERVICE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        mMsgCharacteristic.addDescriptor(
                getClientCharacteristicConfigurationDescriptor());
        mMsgService = new BluetoothGattService(MSG_SERVICE_UUID,BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mMsgService.addCharacteristic(mMsgCharacteristic);
    }

    //usage
    //mMsgCharacteristic.setValue(String);




    public BluetoothGattService getBluetoothGattService() {
        return mMsgService;
    }

    public static BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
        return new BluetoothGattDescriptor(MSG_CHAR_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
    }

    public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
        throw new UnsupportedOperationException("Method writeCharacteristic not overriden");
    }

    public interface ServiceFragmentDelegate {
        void sendNotificationToDevices(BluetoothGattCharacteristic characteristic);
    }
}
