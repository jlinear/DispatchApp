package com.example.marco.bluenet_01.BLE_Comm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.os.SystemClock;
import android.util.Log;
import android.os.Handler;

import com.example.marco.bluenet_01.BuildConfig;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;


import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.Query;
import nd.edu.bluenet_stack.Reader;
import nd.edu.bluenet_stack.MessageRetriever;
import nd.edu.bluenet_stack.AsyncMessageRetriever;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

/**
 * Created by jerry on 6/4/18.
 * Scanning and as GATT client
 */

public class BleReader extends LayerBase
        implements
        Query{

    private static final String ERR_TAG = "FATAL ERROR";
    private static final String INFO_TAG = "APP_INFO";
    private static final String DEBUG_TAG = "DEBUG";

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

    public Context context;
    public Activity activity;
    String originalName;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

   // public List<BluetoothGattService> mBluetoothGattService;
    private Map<String, byte[]> mBTToBNAddrMap = new HashMap<String, byte[]>();
   // public Set<BluetoothDevice> mBleDeviceSet;
    private Map<String, BluetoothGatt> mConnectedDeviceMap = new HashMap<String, BluetoothGatt>();
    private Map<String, Integer> mServiceSetupTables = new HashMap<String, Integer>();

    private Map<String, BlockingQueue<byte []>> mPendingMessage = new HashMap<String, BlockingQueue<byte []>>();


    private Map<String, Boolean> mPendingConnect = new HashMap<String, Boolean>();
    private List<UUID> mCharactericUUIDList = new ArrayList<UUID>();

    private Handler mBGHandler = new Handler();
    private Runnable mRunner = null;
    private ConcurrentLinkedQueue<AdvertisementPayload> mInQ = new ConcurrentLinkedQueue<>();
    private boolean mStarted = false;

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

    private int getMsgType(UUID uuid) {
        int msgType = 0;
        if (SMALL_MSG_CHAR_UUID.equals(uuid) ) {
            msgType = AdvertisementPayload.SMALL_MESSAGE;
        }
        else if (REGULAR_MSG_CHAR_UUID.equals(uuid)) {
            msgType = AdvertisementPayload.REGULAR_MESSAGE;
        }
        else if (LOCATION_UPDATE_CHAR_UUID.equals(uuid)) {
            msgType = AdvertisementPayload.LOCATION_UPDATE;
        }
        else if (GROUP_QUERY_CHAR_UUID.equals(uuid)) {
            msgType = AdvertisementPayload.GROUP_QUERY;
        }
        else if (GROUP_UPDATE_CHAR_UUID.equals(uuid)) {
            msgType = AdvertisementPayload.GROUP_UPDATE;
        }

        return msgType;
    }


    public BleReader(Context context, Activity activity){
        mCharactericUUIDList.add(SMALL_MSG_CHAR_UUID);
        mCharactericUUIDList.add(REGULAR_MSG_CHAR_UUID);
        mCharactericUUIDList.add(LOCATION_UPDATE_CHAR_UUID);
        mCharactericUUIDList.add(GROUP_QUERY_CHAR_UUID);
        mCharactericUUIDList.add(GROUP_UPDATE_CHAR_UUID);

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
        mBluetoothManager = (BluetoothManager) activity.getSystemService(context.BLUETOOTH_SERVICE);
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

        mRunner = new Runnable () {
            @Override
            public void run() {
                AdvertisementPayload toRead = mInQ.poll();

                if (null != toRead) {
                    mReadCB.read(toRead);
                }

                mBGHandler.postDelayed(this, 30);
            }
        };

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
                .setServiceUuid(new ParcelUuid(BLUENET_SERVICE_UUID))
                .build();

        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(ResultsFilter);
        Log.i(INFO_TAG,"BLE SCAN STARTED");

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
    };

    private void processResult(ScanResult result){
        BluetoothDevice device = result.getDevice();
        String deviceAddr = device.getAddress();

        //get the remote device
        BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(deviceAddr);
        //check the connection state with the remote device
        int connectionState = mBluetoothManager.getConnectionState(remoteDevice, BluetoothProfile.GATT);

        //if we are disconnected then proceed
        if(connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            //we disconnected at some point so remove the device from
            //our map of devices
            if (mConnectedDeviceMap.containsKey(deviceAddr)) {
                mConnectedDeviceMap.remove(deviceAddr);
            }
            //if we have not tried to connect to this device before marker that
            //we are not currently handling a pending connection operation
            if (null == mPendingConnect.get(deviceAddr)) {
                mPendingConnect.put(deviceAddr, false);
            }

            //if we are not handling a pending connection then mark that we are now
            //connect to the Gatt server
            if (false == mPendingConnect.get(deviceAddr)) {              
                // connect your device
                 Log.i("BlueNet", "trying to connect!");
                mPendingConnect.put(deviceAddr, true);
                /*BluetoothGatt gatt = */device.connectGatt(this.context, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            }
        }else if( connectionState == BluetoothProfile.STATE_CONNECTED ){
            // already connected . send Broadcast if needed
        }
        

    }


    /**** **** end of BLE SCAN **** ****/


    /**** **** GATT Client **** ****/
    private void setupCharacteristic(BluetoothGatt gatt) {
        Integer index = mServiceSetupTables.get(gatt.getDevice().getAddress());

        if (index < mCharactericUUIDList.size()) {
            //get the UUID of the current characteristic 
             UUID currentUUID = mCharactericUUIDList.get(mServiceSetupTables.get(gatt.getDevice().getAddress()));
            //Enable notifications locally for all msg type characteristics
            

            BluetoothGattService service = gatt.getService(BLUENET_SERVICE_UUID);

            if (null == service) {
                Log.i("BlueNet", "Service is null!");
            }
            else
            {
                Log.i("BlueNet", "Enable characteristic notifications");
                for (UUID uuid : mCharactericUUIDList) {
                     BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
                
                    
                    // Enable notifications for this characteristic locally
                    gatt.setCharacteristicNotification(characteristic, true);
                }
                //get the descriptor (only need to do this once)
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(mCharactericUUIDList.get(0));

                //get the descriptor for the characteristic
                // Write on the config descriptor to be notified when the value changes
                BluetoothGattDescriptor descriptor =
                        characteristic.getDescriptor(CLIENT_CHAR_CONFI_UUID);
                
                //Set the value of the descriptor to notification enabled
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);


                //mServiceSetupTables.put(gatt.getDevice().getAddress(), index + 1);

                //write the descriptor
                gatt.writeDescriptor(descriptor);
            }

        }
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            final BluetoothDevice device = gatt.getDevice();

            String address = device.getAddress();

            //now that we have a connection result we can go ahead and
            //set the pending connect value to false
            mPendingConnect.put(address, false);

            Log.i(INFO_TAG, String.format("onConnectionStatechange-- status: %d, state: %d", status, newState));

            if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {

                Log.i(INFO_TAG, "Connected to GATT server.");

                //if we do not already have an object reference then we need to grab
                //one to keep track of the devices to which we're connected
                if (!mConnectedDeviceMap.containsKey(address)) {
                    mConnectedDeviceMap.put(address, gatt);
                }

                // Discover the services on the gatt server
                Log.i(INFO_TAG, "Attempting to start service discovery:" +
                gatt.discoverServices());

            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(INFO_TAG, "Disconnected from GATT server.");

                //if we get a disconnect state and we have a reference to a device object
                //then we need to close the connection to the server and remove the device
                //from our map
                if (mConnectedDeviceMap.containsKey(address)){
                    BluetoothGatt bluetoothGatt = mConnectedDeviceMap.get(address);
                    if( bluetoothGatt != null ){
                        bluetoothGatt.close();
                        bluetoothGatt = null;
                    }
                    mConnectedDeviceMap.remove(address);
                }

                // if (133 == status) { //bad things! try again!
                //     Handler delayHandler = new Handler();
                //     delayHandler.postDelayed(new Runnable() {
                //         @Override
                //         public void run() {
                //             device.connectGatt(context, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
                //         }
                //     }, 120);

                // }
                // Broadcast if needed
            }


        }

        //when the services have been discovered, we need to set up the notifications
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS) {
                BluetoothDevice device = gatt.getDevice();
                String address = device.getAddress();
                final BluetoothGatt bluetoothGatt = mConnectedDeviceMap.get(address);

                if (!mServiceSetupTables.containsKey(address)) {
                    //only go through this with NEWLY discovered services
                    mServiceSetupTables.put(address, new Integer(0));
                    mBGHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setupCharacteristic(bluetoothGatt);
                        }
                    });
                }
               
               
            } else {
                Log.e(ERR_TAG, "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            //DON'T DO ANYTHING. ONLY SET ONCE!

            // BluetoothDevice device = gatt.getDevice();
            // String address = device.getAddress();
            // final BluetoothGatt bluetoothGatt = mConnectedDeviceMap.get(address);
            // if (CLIENT_CHAR_CONFI_UUID.equals(descriptor.getUuid())) {
            //    mBGHandler.post(new Runnable() {
            //         @Override
            //         public void run() {
            //             setupCharacteristic(bluetoothGatt);
            //         }
            //     });
            // }
        }

        //when we have completed a read request we need to place the result
        //in the appropriate placed mapped to the device who provided the data
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i("BlueNet", "read complete!");

            if (status == GATT_SUCCESS) {
                BluetoothDevice device = gatt.getDevice();
                String address = device.getAddress();
                BluetoothGatt bluetoothGatt = mConnectedDeviceMap.get(address);
                Log.i("BlueNet", "THINGZ!");
                BlockingQueue<byte[]> q = mPendingMessage.get(address);
                try {
                    q.put(characteristic.getValue());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                //mPendingMessage.put(address, q);
            }
            else
            {
                Log.i("BlueNet", "THINGZ FAILED!");
            }
        }

        //when a notification fires, we'll receive the header for message on the
        //appropriate characteristic
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            
            BluetoothDevice device = gatt.getDevice();
            final String address = device.getAddress();
            final BluetoothGatt bluetoothGatt = mConnectedDeviceMap.get(address);
            Log.i("BlueNet", "Characteristic changed from: " + address);

            AdvertisementPayload advPayload = new AdvertisementPayload();
            byte[] data = characteristic.getValue();

            //if can successfully parsed the data then continue
            if (advPayload.fromBytes(data)) {
                //set msgType from the characteristic UUID
                advPayload.setMsgType(getMsgType(characteristic.getUuid()));

                //Get BT-BN addr
                if (advPayload.getTTL() == AdvertisementPayload.MAX_TTL) {
                    mBTToBNAddrMap.put(address, advPayload.getSrcID());
                }

                //Get 1-hop neighbor
                if (mBTToBNAddrMap.containsKey(address)) {
                    advPayload.setOneHopNeighbor(new String(mBTToBNAddrMap.get(address)));
                }

                final BlockingQueue<byte[]> q = new LinkedBlockingQueue<byte[]>(1);
                mPendingMessage.put(address, q);

                //set pull callback which is just a characteristic read
                advPayload.setRetriever(new AsyncMessageRetriever() {
                    @Override
                    public byte [] retrieve(byte [] id){
                        String devAddr = null;
                        for (Map.Entry<String, byte []> entry : mBTToBNAddrMap.entrySet())
                        {
                            if (Arrays.equals(entry.getValue(), id)) {
                                devAddr = entry.getKey();
                                break;
                            }
                        }

                        final BluetoothGatt btGatt = mConnectedDeviceMap.get(devAddr);
                        Log.i("BlueNet", "MessageRetriever doing things!");
                        //grab the characteristic
                        //Log.i("BlueNet", "MessageRetriever doing things!");
                        //grab the characteristic
                        BluetoothGattCharacteristic characteristic = btGatt
                                .getService(BLUENET_SERVICE_UUID)
                                .getCharacteristic(PULL_MESSAGE_CHAR_UUID);
                  

                        //read the characteristic
                        bluetoothGatt.readCharacteristic(characteristic);
                        
                        // Handler someHandler = new Handler(context.getMainLooper());
                        // someHandler.post(new Runnable () {
                        //     public void run() {
                                

                               
                        //     }
                        // });

                        return new byte[0];
                    }

                    @Override
                    public BlockingQueue<byte []> asyncRetrieve(byte [] id){
                        String devAddr = null;
                        for (Map.Entry<String, byte []> entry : mBTToBNAddrMap.entrySet())
                        {
                            if (Arrays.equals(entry.getValue(), id)) {
                                devAddr = entry.getKey();
                                break;
                            }
                        }
                        BlockingQueue<byte []> retQ = mPendingMessage.get(devAddr);

                        if (null == retQ) {
                            Log.e("BlueNet", "Q is null?");
                        }
                    
                        return retQ;
                    }
                });

                //THIS MAY BE THE PROBLEM
                //notify -> read -> read -> msgretriever -> readChar -> wait for result
                // provide payload to the next layer up
                
                mInQ.offer(advPayload);
                if (!mStarted) {
                    mBGHandler.post(mRunner);
                }


                //mReadCB.read(advPayload);
            }

        }
    };


    /**** **** End of Gatt Client **** ****/

    @Override
    public String ask(String question) {
        return null;
    }
}
