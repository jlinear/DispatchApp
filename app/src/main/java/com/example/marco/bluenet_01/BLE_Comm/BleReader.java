package com.example.marco.bluenet_01.BLE_Comm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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
import java.util.List;

import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.Query;
import nd.edu.bluenet_stack.Reader;

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

    public static final ParcelUuid BASIC_AD =   //used as an agreement for ad/sc
            ParcelUuid.fromString("00001860-0000-1000-8000-00805f9b34fb");

    public Context context;
    public Activity activity;
    String originalName;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;


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
