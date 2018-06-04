package com.example.marco.bluenet_01.BLE_Comm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
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
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.marco.bluenet_01.BuildConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LocationEntry;


/**
 * Created by jerry on 4/18/18.
 */

public class BleBasic {

    private static final String ERR_TAG = "FATAL ERROR";
    private static final String INFO_TAG = "APP_INFO";

    /**** **** variable declaration **** ****/
    public Context context;
    public Activity activity;
    String originalName;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothLeScanner mBluetoothLeScanner;

    AdvertisementPayload scan_Payload = new AdvertisementPayload();

    //public static final ParcelUuid



    //the 5-8 digits defines the service
    //List of pre-defined GATT service uuid can be found at:
    //  https://www.bluetooth.com/specifications/gatt/services
    //List of pre-defined GATT characteristics can be found at:
    //  https://www.bluetooth.com/specifications/gatt/characteristics
    public static final ParcelUuid PAYLOAD_SERVICE =   //used as an agreement for ad/sc
            ParcelUuid.fromString("00001868-0000-1000-8000-00805f9b34fb");
    public static String PAYLOAD_CHARACTERISTIC_CONFIG =
            "00002968-0000-1000-8000-00805f9b34fb";

    public HashMap<BluetoothDevice, byte[]> mBleDevicesDict;
    public HashMap<String, Location> ID_Loc_Dict;
    /**** **** end of variable declaration **** ****/


    public BleBasic(Context context, Activity activity){
        this.context = context;
        this.activity = activity;
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

        mBleDevicesDict = new HashMap<BluetoothDevice, byte[]>();
        ID_Loc_Dict = new HashMap<String, Location>();

    }

    void findDevices(int time){
        startLeScanning();
        Handler handler = new Handler();
        final Runnable r = new Runnable(){
            public void run() {
                stopScanning();
            }
        };

        handler.postDelayed(r, time);
    }

    /**** **** BLE ADVERTISE **** ****/
    public void startLeAdvertising(byte[] data_out){
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //3 modes: LOW_POWER, BALANCED, LOW_LATENCY
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) // ULTRA_LOW, LOW, MEDIUM, HIGH
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(PAYLOAD_SERVICE)
                .addServiceData(PAYLOAD_SERVICE,data_out)
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
    /**** **** end of BLE ADVERTISE **** ****/


    /**** **** BLE SCAN **** ****/
    public void startLeScanning(){
        //scan filters
        ScanFilter ResultsFilter = new ScanFilter.Builder()
                //.setDeviceAddress(string)
                //.setDeviceName(string)
                //.setManufacturerData()
                //.setServiceData()
                .setServiceUuid(PAYLOAD_SERVICE)
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
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //LOW_POWER, BALANCED, LOW_LATENCY
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
            byte[] payload = result.getScanRecord().getServiceData().get(PAYLOAD_SERVICE);
            scan_Payload.fromBytes(payload);
//            Log.d(INFO_TAG,"scan payload " + new String(payload,StandardCharsets.UTF_8));
            String userID = new String(scan_Payload.getSrcID(), StandardCharsets.UTF_8);
            if(!ID_Loc_Dict.containsKey(userID)){
                Location temp_loc = null;
                ID_Loc_Dict.put(userID,temp_loc);
                Log.d(INFO_TAG,"Found a new user nearby!" + " " + userID);

            }
        }
    };

//        private void processResult(ScanResult result) {
//            //Log.v(ADSCActivity.class.getSimpleName(), "Process scan results");
//            if(!mBleDevicesDict.containsKey(result.getDevice())) {
//
//                byte[] scan_payload = result.getScanRecord().getServiceData().get(PAYLOAD_SERVICE);
//                advPayload.fromBytes(scan_payload);
////                Double Lat = AdvertisementPayload.parse_scan_payload(scan_payload).getLatitude();
////                Double Long = AdvertisementPayload.parse_scan_payload((scan_payload)).getLongitude();
////                String userID = AdvertisementPayload.parse_scan_payload(scan_payload).getProvider();
////
//////                String payload = new String(result.getScanRecord().getServiceData().get(PAYLOAD_SERVICE));
////                Log.d("DEBUG",result.getDevice().getName() + ' ' + Lat + ',' + Long + ';' + userID);
////                mBleDevicesDict.put(result.getDevice(), scan_payload);
//            }
//
//            //Log.d(DEBUG_TAG,"# of available devices: " + Integer.toString(mBleDevicesDict.size()));
//            //mBluetoothGatt = result.getDevice().connectGatt(ADSCActivity.this,false,mGattCallback);
//        }
//    };
    /**** **** end of BLE SCAN **** ****/

}
