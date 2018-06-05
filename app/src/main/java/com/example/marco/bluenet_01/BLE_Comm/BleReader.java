package com.example.marco.bluenet_01.BLE_Comm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.marco.bluenet_01.BuildConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.Query;
import nd.edu.bluenet_stack.Reader;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

/**
 * Created by jerry on 6/4/18.
 * Scanning and as GATT client
 */

public class BleReader extends LayerBase
        implements
        Reader,
        Query{

    private static final String ERR_TAG = "FATAL ERROR";
    private static final String INFO_TAG = "APP_INFO";
    private static final String DEBUG_TAG = "DEBUG";

    public static final ParcelUuid BASIC_AD =   //used as an agreement for ad/sc
            ParcelUuid.fromString("00001860-0000-1000-8000-00805f9b34fb");
    private static final UUID MSG_SERVICE_UUID = UUID
            .fromString("00001869-0000-1000-8000-00805f9b34fb");
    private static final UUID MSG_CHAR_UUID = UUID.
            fromString("00002a09-0000-1000-8000-00805f9b34fb");

    public Context context;
    public Activity activity;
    String originalName;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    public BluetoothGatt mBluetoothGatt;
//    private BluetoothGattCallback mGattCallback;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public List<BluetoothGattService> mBluetoothGattService;
//    public HashMap<BluetoothDevice, byte[]> mBleDevicesDict;
    public Set<BluetoothDevice> mBleDeviceSet;


    public BleReader(Context context, Activity activity){
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

        startLeScanning();
//        mBleDevicesDict = new HashMap<BluetoothDevice, byte[]>();
        mBleDeviceSet = new HashSet<BluetoothDevice>();

    }

    /**** **** BLE SCAN **** ****/
    public void startLeScanning(){
        //scan filters
        ScanFilter ResultsFilter = new ScanFilter.Builder()
                //.setDeviceAddress(string)
                //.setDeviceName(string)
                //.setManufacturerData()
                //.setServiceData()
                .setServiceUuid(BASIC_AD)
                .build();

        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(ResultsFilter);
        Log.d(INFO_TAG,"BLE SCAN STARTED");

        //scan settings
        ScanSettings settings = new ScanSettings.Builder()
                //.setCallbackType() //int
                //.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE) //AGGRESSIVE, STICKY  //require API 23
                //.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT) //ONE, FEW, MAX  //require API 23
                //.setReportDelay(0) //0: no delay; >0: queue up
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) //LOW_POWER, BALANCED, LOW_LATENCY
                .build();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothLeScanner == null){
            Log.e(ERR_TAG, "no BLE scanner assigned!!!");
            return;
        }
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
    }

    public void stopScanning() {
        if (mBluetoothLeScanner != null)
            mBluetoothLeScanner.stopScan(mScanCallback);
        Log.i("BLE","LE scan stopped");
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        //callbackType could be one of
        // CALLBACK_TYPE_ALL_MATCHES, CALLBACK_TYPE_FIRST_MATCH or CALLBACK_TYPE_MATCH_LOST
        public void onScanResult(int callbackType, ScanResult result) {
//            Log.d(INFO_TAG, "onScanResult: callbackType " + Integer.toString(callbackType));
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i(INFO_TAG, "onBatchScanResults: " + results.size() + " results");
            for (ScanResult result : results) {
                processResult(result);
            }
        }


        @Override
        public void onScanFailed(int errorCode) {
            Log.e(ERR_TAG, "LE Scan Failed: " + errorCode);
        }

        private void processResult(ScanResult result){
            BluetoothDevice tempDevice = result.getDevice();
            if(!mBleDeviceSet.contains(tempDevice)){
                mBleDeviceSet.add(tempDevice);
                Log.d(DEBUG_TAG,"Found a new device " + result.getDevice().getAddress());
                connect(tempDevice);
            }

//            byte[] payload = result.getScanRecord().getServiceData().get(PAYLOAD_SERVICE);
//            scan_Payload.fromBytes(payload);
////            Log.d(INFO_TAG,"scan payload " + new String(payload,StandardCharsets.UTF_8));
//            String userID = new String(scan_Payload.getSrcID(), StandardCharsets.UTF_8);
//            if(!ID_Loc_Dict.containsKey(userID)){
//                Location temp_loc = null;
//                ID_Loc_Dict.put(userID,temp_loc);
//                Log.d(INFO_TAG,"Found a new user nearby!" + " " + userID + result.getDevice().getAddress());
//
//            }
//        }
        }
    };
    /**** **** end of BLE SCAN **** ****/


    /**** **** GATT Client **** ****/
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
//                broadcastUpdate("connected");
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
                gatt.readCharacteristic(gatt.getService(MSG_SERVICE_UUID).getCharacteristic(MSG_CHAR_UUID));
            } else {
                Log.e(ERR_TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == GATT_SUCCESS) {
                Log.d(DEBUG_TAG,"read returns: " + new String(characteristic.getValue(), StandardCharsets.UTF_8));
                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            broadcastUpdate("char_changed", characteristic);
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

    public boolean connect(final BluetoothDevice mDevice){
        if (mBluetoothAdapter == null) {
            Log.w(ERR_TAG, "BluetoothAdapter not initialized.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothGatt != null) {
            Log.d(INFO_TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
//
//        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice().getAddress();
//        if (device == null) {
//            Log.w(TAG, "Device not found.  Unable to connect.");
//            return false;
//        }

            //BluetoothDevice cdevice = mBluetoothAdapter.getRemoteDevice(mDevice.getAddress());
            mBluetoothGatt = mDevice.connectGatt(context, false, mGattCallback);
            return true;

    }

    public void readLargeChar(){
        if(mConnectionState == STATE_CONNECTED){
            BluetoothGattCharacteristic mChar = mBluetoothGatt.getService(MSG_SERVICE_UUID).getCharacteristic(MSG_CHAR_UUID);
            mBluetoothGatt.readCharacteristic(mChar);
        }
    }
    /**** **** End of Gatt Client **** ****/

    @Override
    public int read(String src, byte[] message) {
        return 0;
    }

    @Override
    public int read(AdvertisementPayload advPayload) {
        return 0;
    }

    @Override
    public String ask(String question) {
        return null;
    }
}
