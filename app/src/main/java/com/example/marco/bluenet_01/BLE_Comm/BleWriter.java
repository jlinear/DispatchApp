package com.example.marco.bluenet_01.BLE_Comm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.marco.bluenet_01.BuildConfig;

import java.net.ContentHandler;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.LayerIFace;
import nd.edu.bluenet_stack.Query;
import nd.edu.bluenet_stack.Writer;

/**
 * Created by jerry on 6/4/18.
 * Advertising and as GATT server
 */

public class BleWriter extends LayerBase
        implements
        Writer,
        Query{

    private static final String ERR_TAG = "FATAL ERROR";
    private static final String INFO_TAG = "APP_INFO";

    public Context context;
    public Activity activity;
    String originalName;

    public static final ParcelUuid BASIC_AD =   //used as an agreement for ad/sc
            ParcelUuid.fromString("00001860-0000-1000-8000-00805f9b34fb");
    private static final UUID MSG_SERVICE_UUID = UUID
            .fromString("00001869-0000-1000-8000-00805f9b34fb");
    private static final UUID MSG_CHAR_UUID = UUID.
            fromString("00002a09-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHAR_CONFI_UUID = UUID.
            fromString("00002a08-0000-1000-8000-00805f9b34fb");




    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;
    private BluetoothGattService mBluetoothGattService;
    private BluetoothGattCharacteristic mMSGChar;

    public BleWriter(Context context, Activity activity){
        this.context = context.getApplicationContext();
        this.activity = activity.getParent();
        this.originalName = activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("originalName", "");

        // Use this to check if BLE is supported on the device.
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(ERR_TAG, "BLE not supported!");
            activity.finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager mBluetoothManager =
                (BluetoothManager) activity.getSystemService(context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            //Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            Log.e(ERR_TAG, "Bluetooth not supported!");
            activity.finish();
            return;
        }
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        startLeAdvertising();
        mGattServer = mBluetoothManager.openGattServer(context,mGattServerCallback);
        mBluetoothGattService = new BluetoothGattService(MSG_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mMSGChar = new BluetoothGattCharacteristic(MSG_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE| BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE|BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor mDescriptor = new BluetoothGattDescriptor(CLIENT_CHAR_CONFI_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        mMSGChar.addDescriptor(mDescriptor);
        mBluetoothGattService.addCharacteristic(mMSGChar);
        mGattServer.addService(mBluetoothGattService);

    }

    /**** **** BLE ADVERTISE **** ****/
    public void startLeAdvertising(byte[] data_out){ //with adv payload
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //3 modes: LOW_POWER, BALANCED, LOW_LATENCY
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) // ULTRA_LOW, LOW, MEDIUM, HIGH
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(BASIC_AD)
                .addServiceData(BASIC_AD,data_out)
                .build();

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null){
            Log.e(ERR_TAG, "no BLE advertiser assigned!!!");
            return;
        }
        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    public void startLeAdvertising(){ // without adv payload
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //3 modes: LOW_POWER, BALANCED, LOW_LATENCY
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) // ULTRA_LOW, LOW, MEDIUM, HIGH
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(BASIC_AD)
//                .addServiceData(BASIC_AD)
                .build();

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null){
            Log.e(ERR_TAG, "no BLE advertiser assigned!!!");
            return;
        }
        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(INFO_TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(ERR_TAG, "LE Advertise Failed: " + errorCode);
        }
    };

    public void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        Log.i(INFO_TAG,"LE Advertise Stopped.");
    }

    public void restartLeAdvertising(byte[] data_out) {
        stopAdvertising();
        startLeAdvertising(data_out);
    }

    public void restartLeAdvertising() {
        stopAdvertising();
        startLeAdvertising();
    }
    /**** **** end of BLE ADVERTISE **** ****/


    /**** **** GATT Server **** ****/
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS){
                if (newState == BluetoothGatt.STATE_CONNECTED){
                    //mDevice = device;
                    Log.d(INFO_TAG,"Gatt Server connected.");
                }
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value);
//            rec_msg = new String(value, StandardCharsets.UTF_8);
//            w_state.setBoo(!b_state);
//            Log.v("chatFrag", "Characteristic Write request: " + Arrays.toString(value) + rec_msg);


            int status = 0;
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with an offset */ 0,
            /* No need to respond with a value */ null);
            }
        }
    };
    /**** **** End of Gatt Server **** ****/


    @Override
    public int write(AdvertisementPayload advPayload) {
        return 0;
    }

    @Override
    public int write(String dest, byte[] message) {
        return 0;
    }

    @Override
    public String ask(String question) {
        return null;
    }
}
