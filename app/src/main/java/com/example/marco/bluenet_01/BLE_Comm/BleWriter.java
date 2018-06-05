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
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


import android.os.Handler;
import java.util.concurrent.ConcurrentLinkedQueue;


import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.Query;
import nd.edu.bluenet_stack.Writer;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattDescriptor.PERMISSION_WRITE;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

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

//    public static final ParcelUuid BASIC_AD =   //used as an agreement for ad/sc
//            ParcelUuid.fromString("00001860-0000-1000-8000-00805f9b34fb");

    //service UUID
    public static final UUID BLUENET_SERVICE_UUID =
            UUID.fromString("00001860-0000-1000-8000-00805f9b34fb");


    //UUIDs for different messsage types
    private static final UUID SMALL_MSG_CHAR_UUID =
            UUID.fromString("0000" + AdvertisementPayload.SMALL_MESSAGE_STR + "-0000-1000-8000-00805f9b34fb");
    private static final UUID REGULAR_MSG_CHAR_UUID =
            UUID.fromString("0000" + AdvertisementPayload.REGULAR_MESSAGE_STR + "-0000-1000-8000-00805f9b34fb");
    private static final UUID LOCATION_UPDATE_CHAR_UUID =
            UUID.fromString("0000" + AdvertisementPayload.LOCATION_UPDATE_STR + "-0000-1000-8000-00805f9b34fb");
    private static final UUID GROUP_QUERY_CHAR_UUID =
            UUID.fromString("0000" + AdvertisementPayload.GROUP_QUERY_STR + "-0000-1000-8000-00805f9b34fb");
    private static final UUID GROUP_UPDATE_CHAR_UUID =
            UUID.fromString("0000" + AdvertisementPayload.GROUP_UPDATE_STR + "-0000-1000-8000-00805f9b34fb");

    //UUID for messages that are pulled
    private static final UUID PULL_MESSAGE_CHAR_UUID =
            UUID.fromString("31517c58-66bf-470c-b662-e352a6c80cba");


    //for configuring notifications
    private static final UUID CLIENT_CHAR_CONFI_UUID = UUID.
            fromString("00002a08-0000-1000-8000-00805f9b34fb");



    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;


    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    private ConcurrentLinkedQueue<AdvertisementPayload> mOutQ = new ConcurrentLinkedQueue<>();
    private boolean mStarted = false;
    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    //For handling pulled messages
    private byte [] mCurrentMsg = null;
    private Timestamp mLastRead = null;
    private final long mReadTimeout = 60L;

    private BluetoothGattService createService() {
        BluetoothGattService service = new BluetoothGattService(BLUENET_SERVICE_UUID, SERVICE_TYPE_PRIMARY);

        // Message type characteristics (read-only, supports subscriptions)
        BluetoothGattCharacteristic smallMsg = new BluetoothGattCharacteristic(SMALL_MSG_CHAR_UUID, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
        BluetoothGattDescriptor headerConfig = new BluetoothGattDescriptor(CLIENT_CHAR_CONFI_UUID, PERMISSION_READ | PERMISSION_WRITE);
        smallMsg.addDescriptor(headerConfig);

        BluetoothGattCharacteristic regMsg = new BluetoothGattCharacteristic(REGULAR_MSG_CHAR_UUID, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
        regMsg.addDescriptor(headerConfig);

        BluetoothGattCharacteristic locUpdate = new BluetoothGattCharacteristic(LOCATION_UPDATE_CHAR_UUID, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
        locUpdate.addDescriptor(headerConfig);

        BluetoothGattCharacteristic grpQuery = new BluetoothGattCharacteristic(GROUP_QUERY_CHAR_UUID, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
        grpQuery.addDescriptor(headerConfig);

        BluetoothGattCharacteristic grpUpdate = new BluetoothGattCharacteristic(GROUP_UPDATE_CHAR_UUID, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
        grpUpdate.addDescriptor(headerConfig);

        //pull characteristic is read-only with no notify available
        BluetoothGattCharacteristic pullMsg = new BluetoothGattCharacteristic(PULL_MESSAGE_CHAR_UUID, PROPERTY_READ, PERMISSION_READ);


        service.addCharacteristic(smallMsg);
        service.addCharacteristic(regMsg);
        service.addCharacteristic(locUpdate);
        service.addCharacteristic(grpQuery);
        service.addCharacteristic(grpUpdate);
        service.addCharacteristic(pullMsg);

        return service;
    }

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

        mGattServer.addService(createService());

        mRunnable = new Runnable() {
            @Override
            public void run() {

                //if devices are still trying to pull the message then don't try to
                //change it
                if (mLastRead != null
                        && mReadTimeout > (mLastRead.getTime() - System.currentTimeMillis())) {
                    AdvertisementPayload toSend = mOutQ.poll();

                    if (null != toSend) {
                        byte[] header = toSend.getHeader();
                        mCurrentMsg = toSend.getMsg();
                        int type = toSend.getMsgType();

                        //Now write to my characteristic
                        //iterate over list of devices and notify
                        byte[] nextHopNeighbor = toSend.getOneHopNeighbor();
                        if (null != nextHopNeighbor) {
                            //only notify specified neighbor
                        } else {
                            notifyRegisteredDevices(type, header);
                        }
                    }
                }
                mHandler.postDelayed(mRunnable, 30);
            }
        };
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
                .addServiceUuid(new ParcelUuid(BLUENET_SERVICE_UUID))
                .addServiceData(new ParcelUuid(BLUENET_SERVICE_UUID),data_out)
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
                .addServiceUuid(new ParcelUuid(BLUENET_SERVICE_UUID))
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
        public void onCharacteristicReadRequest(BluetoothDevice device,
                                                int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (PULL_MESSAGE_CHAR_UUID.equals(characteristic.getUuid())) {
                mLastRead = new Timestamp(System.currentTimeMillis());

                //https://stackoverflow.com/questions/29512305/android-ble-peripheral-oncharacteristicread-return-wrong-value-or-part-of-it
                if (offset > mCurrentMsg.length) {
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{0});
                }
                else {
                    int size = mCurrentMsg.length - offset;
                    byte[] response = new byte[size];

                    for (int i = offset; i < mCurrentMsg.length; i++) {
                        response[i - offset] = mCurrentMsg[i];
                    }

                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response);
                }
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (CLIENT_CHAR_CONFI_UUID.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(INFO_TAG, "Subscribe device to notifications: " + device);
                    mRegisteredDevices.add(device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(INFO_TAG, "Unsubscribe device from notifications: " + device);
                    mRegisteredDevices.remove(device);
                }

                if (responseNeeded) {
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }
            } else {
                Log.w(INFO_TAG, "Unknown descriptor write request");
                if (responseNeeded) {
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            }
        }

    };


    private UUID getUUID(int msgType) {
        switch (msgType) {
            case AdvertisementPayload.SMALL_MESSAGE:
                return SMALL_MSG_CHAR_UUID;
            case AdvertisementPayload.REGULAR_MESSAGE:
                return REGULAR_MSG_CHAR_UUID;
            case AdvertisementPayload.LOCATION_UPDATE:
                return LOCATION_UPDATE_CHAR_UUID;
            case AdvertisementPayload.GROUP_QUERY:
                return GROUP_QUERY_CHAR_UUID;
            case AdvertisementPayload.GROUP_UPDATE:
                return GROUP_UPDATE_CHAR_UUID;
            default:
                return null;
        }
    }

    //private void notifyRegisteredDevice()

    private void notifyRegisteredDevices(int msgType, byte [] data) {
        UUID charUUID = getUUID(msgType);
        if (null != charUUID) {
            BluetoothGattCharacteristic characteristic = mGattServer
                    .getService(BLUENET_SERVICE_UUID)
                    .getCharacteristic(charUUID);

            for (BluetoothDevice device : mRegisteredDevices) {
                characteristic.setValue(data);
                mGattServer.notifyCharacteristicChanged(device, characteristic, false);
            }
        }
    }

    /**** **** End of Gatt Server **** ****/

    @Override
    public int write(AdvertisementPayload advPayload) {
        mOutQ.add(advPayload);
        if (!mStarted) {
            mRunnable.run();
            mStarted = true;
        }

        return 0;
    }

    @Override
    public int write(String dest, byte[] message) {
        throw new java.lang.UnsupportedOperationException("Not supported.");
    }


    @Override
    public String ask(String question) {
        String resultString = new String();


        return resultString;
    }
}
