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


import com.example.marco.bluenet_01.BuildConfig;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;

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

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

/**
 * Created by jerry on 6/4/18.
 * Scanning and as GATT client
 */

public class BleReader extends LayerBase
        implements
        Query,
        Handler.Callback{

    private static final String ERR_TAG = "FATAL ERROR";
    private static final String INFO_TAG = "APP_INFO";
    private static final String DEBUG_TAG = "DEBUG";

    private static final int MSG_CONNECT = 10;
    private static final int MSG_CONNECTED = 20;
    private static final int MSG_DISCONNECT = 30;
    private static final int MSG_DISCONNECTED = 40;
    private static final int MSG_SERVICES_DISCOVERED = 50;
    private static final int MSG_NOTIFIED = 60;
    private static final int MSG_READ = 70;
    private static final int MSG_REGISTER = 80;
    private static final int MSG_REGISTERED = 90;
    private static final int MSG_READ_REQ = 100;



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

   // public List<BluetoothGattService> mBluetoothGattService;
    private Map<String, byte[]> mBTToBNAddrMap = new HashMap<String, byte[]>();
   // public Set<BluetoothDevice> mBleDeviceSet;
    private Map<String, BluetoothGatt> mConnectedDeviceMap = new HashMap<String, BluetoothGatt>();
    private Map<String, Boolean> mActiveDevices = new HashMap<String, Boolean>();

    private Map<String, Integer> mServiceSetupTables = new HashMap<String, Integer>();

    private Map<String, BlockingQueue<byte []>> mPendingMessage = new HashMap<String, BlockingQueue<byte []>>();


    private Map<String, Boolean> mPendingConnect = new HashMap<String, Boolean>();
    private List<UUID> mCharactericUUIDList = new ArrayList<UUID>();

    private Handler mBGHandler = new Handler();
    private Runnable mRunner = null;
    private final long RUNNER_DELAY = 30;
    private int mRunCounter = 0;
    private ConcurrentLinkedQueue<AdvertisementPayload> mInQ = new ConcurrentLinkedQueue<>();
    private boolean mStarted = false;


    private Handler mainHandler = new Handler(Looper.getMainLooper(), this);
    private Handler bleHandler;
    private Context context;
    private MyBleCallback myBleCallback = new MyBleCallback();
    private Set<BleCallback> listeners = new HashSet<>();

    public interface BleCallback {
        /**
         * Signals that the BLE device is ready for communication.
         */
        @MainThread
        void onDeviceReady();

        /**
         * Signals that a connection to the device was lost.
         */
        @MainThread
        void onDeviceDisconnected();
    }

    public class ReadResult {
        public BluetoothGatt gatt;
        public BluetoothGattCharacteristic characteristic;

        public ReadResult(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            this.gatt = gatt;
            this.characteristic = characteristic;
        }
    }

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

        this.context = context;//.getApplicationContext();
        this.activity = activity.getParent();
        this.originalName = activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("originalName", "");

        // Use this to check if BLE is supported on the device.
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(ERR_TAG, "BLE not supported!");
            activity.finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
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

                // Check to make sure our connections are active. Otherwise, disconnect
                // and let the reconnect fix whatever is going on
                if (mRunCounter == 333) { //roughly 10 seconds--2x location update period
                    for (Map.Entry<String, Boolean> entry : mActiveDevices.entrySet()) {
                        if (entry.getValue()) {
                            entry.setValue(false);
                        }
                        else {
                            Log.i("BlueNet", "Device " + entry.getKey() + " inactive. Disconnecting...");
                            // BluetoothGatt gatt = mConnectedDeviceMap.get(entry.getKey());
                            // gatt.disconnect();
                            // mActiveDevices.remove(entry.getKey());
                        }
                    }

                    mRunCounter = 0;
                }

                mRunCounter++;
                mBGHandler.postDelayed(this, RUNNER_DELAY);
            }
        };

        startLeScanning();
    }


    @MainThread
    public void addListener(BleCallback bleCallback) {
        listeners.add(bleCallback);
    }

    @MainThread
    public void removeListener(BleCallback bleCallback) {
        listeners.remove(bleCallback);
    }

    //Not sure if these will be useful ***********
    private void connect(BluetoothDevice autoConnect) {
       
    }

    private void disconnect(BluetoothDevice device) {
        bleHandler.obtainMessage(MSG_DISCONNECT, device).sendToTarget();
    }
    //********************************************
   
    @Override
    public boolean handleMessage(Message message) {
      switch (message.what) {
        case MSG_CONNECT:
            doConnect((ScanResult)message.obj);
            break;
        case MSG_CONNECTED:
            doConnected((BluetoothGatt)message.obj);
            break;
        case MSG_DISCONNECT:
          
          break;
        case MSG_DISCONNECTED:
            doDisconnected((BluetoothGatt)message.obj);
            break;
        case MSG_SERVICES_DISCOVERED:
            doServicesDiscovered((BluetoothGatt)message.obj);
            break;
        case MSG_NOTIFIED:
            doNotified((ReadResult)message.obj);
            break;
        case MSG_READ:
            doRead((ReadResult)message.obj);
            break;
        case MSG_READ_REQ:
            
            break;
        case MSG_REGISTER:
            doRegister((BluetoothGatt)message.obj);
            break;
        case MSG_REGISTERED:
            doRegistered((BluetoothGatt)message.obj);
            doNotifyReady();
      }
      return true;
    }

    @MainThread
     private void doConnect(ScanResult result){
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
                remoteDevice.connectGatt(this.context, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            }
        }else if( connectionState == BluetoothProfile.STATE_CONNECTED ){
            // already connected . send Broadcast if needed
        }
        

    }

    @MainThread
    private void doConnected (BluetoothGatt gatt) {
        BluetoothDevice device = gatt.getDevice();
        String address = device.getAddress();

        Log.i(INFO_TAG, "Connected to GATT server at " + address);

        //if we do not already have an object reference then we need to grab
        //one to keep track of the devices to which we're connected
        if (!mConnectedDeviceMap.containsKey(address)) {
            mConnectedDeviceMap.put(address, gatt);
        }

        // Discover the services on the gatt server
        Log.i(INFO_TAG, "Starting service discovery" +
        gatt.discoverServices());
    }

    @MainThread
    private void doDisconnected(BluetoothGatt gatt) {
        BluetoothDevice device = gatt.getDevice();
        String address = device.getAddress();
        Log.i(INFO_TAG, "Disconnected from GATT server at " + address);

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
            mServiceSetupTables.remove(address);
        }
        
    }

    @MainThread
    private void doNotifyReady() {
        for (BleCallback listener : listeners) {
          listener.onDeviceReady();
        }
    }

    @MainThread
    private void doServicesDiscovered(BluetoothGatt gatt) {
        BluetoothDevice device = gatt.getDevice();
        String address = device.getAddress();
        final BluetoothGatt bluetoothGatt = mConnectedDeviceMap.get(address);

        if (!mServiceSetupTables.containsKey(address)) {
            //only go through this with NEWLY discovered services
            mServiceSetupTables.put(address, new Integer(0));
            bleHandler.obtainMessage(MSG_REGISTER, bluetoothGatt).sendToTarget();
        }
    }

    @MainThread
    private void doNotified(ReadResult readRes) {
        BluetoothDevice device = readRes.gatt.getDevice();
        final String address = device.getAddress();
        final BluetoothGatt bluetoothGatt = mConnectedDeviceMap.get(address);
        Log.i("BlueNet", "Characteristic change from: " + address);
        mActiveDevices.put(address, true); //we've seen something!

        AdvertisementPayload advPayload = new AdvertisementPayload();
        byte[] data = readRes.characteristic.getValue();

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
            advPayload.setRetriever(new MessageRetriever() {
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
                    Log.i("BlueNet", "MessageRetriever starting read request");
                   
                    //grab the characteristic
                    BluetoothGattCharacteristic characteristic = btGatt
                            .getService(BLUENET_SERVICE_UUID)
                            .getCharacteristic(PULL_MESSAGE_CHAR_UUID);
              

                    //read the characteristic
                    bluetoothGatt.readCharacteristic(characteristic);

                    BlockingQueue<byte []> retQ = mPendingMessage.get(devAddr);

                    if (null == retQ) {
                        Log.e("BlueNet", "Pending message queue is null.");
                    }

                    byte [] retMsg = new byte [0];
                    try {
                        retMsg = retQ.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                   
                    return retMsg;
                }

            });

            
            mInQ.offer(advPayload);
            if (!mStarted) {
                mBGHandler.post(mRunner);
                mStarted = true;
            }

        }
    }

    @MainThread
    private void doRead(ReadResult readRes) {
       BluetoothDevice device = readRes.gatt.getDevice();
        String address = device.getAddress();
        BluetoothGatt bluetoothGatt = mConnectedDeviceMap.get(address);
        Log.i("BlueNet", "read complete!");
        BlockingQueue<byte[]> q = mPendingMessage.get(address);
        try {
            q.put(readRes.characteristic.getValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @MainThread
    private void doRegister(BluetoothGatt gatt) {
        
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

            //write the descriptor
            gatt.writeDescriptor(descriptor);
        }
    }

    @MainThread
    private void doRegistered(BluetoothGatt gatt){
        //NOTHING!
    }

    /**** **** BLE SCAN **** ****/
    public void startLeScanning(){
        //scan filters
        ScanFilter ResultsFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(BLUENET_SERVICE_UUID))
                .build();

        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(ResultsFilter);
        Log.i(INFO_TAG,"BLE SCAN STARTED");

        //scan settings
        ScanSettings settings = new ScanSettings.Builder()
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
            bleHandler.obtainMessage(MSG_CONNECT, result).sendToTarget();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i(INFO_TAG, "onBatchScanResults: " + results.size() + " results");
            for (ScanResult result : results) {
                bleHandler.obtainMessage(MSG_CONNECT, result).sendToTarget();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(ERR_TAG, "LE Scan Failed: " + errorCode);
        }
    };
    /**** **** end of BLE SCAN **** ****/


    /**** **** GATT Client **** ****/
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //now that we have a connection result we can go ahead and
            //set the pending connect value to false
            mPendingConnect.put(address, false);

            if (BluetoothGatt.GATT_SUCCESS == status) {
                switch(newState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        bleHandler.obtainMessage(MSG_CONNECTED, gatt).sendToTarget();
                        break;

                    case BluetoothGatt.STATE_DISCONNECTED:
                        bleHandler.obtainMessage(MSG_DISCONNECTED, gatt).sendToTarget();
                        break;                
                }
            }
            else {
                Log.e("BlueNet", String.format("Error in onConnectionStatechange -- status: %d, state: %d", status, newState));
            }
        }

        //when the services have been discovered, we need to set up the notifications
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS) {
                bleHandler.obtainMessage(MSG_SERVICES_DISCOVERED, gatt).sendToTarget();  
            } else {
                Log.e(ERR_TAG, "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            //DON'T DO ANYTHING. ONLY SET ONCE!
            bleHandler.obtainMessage(MSG_REGISTERED, gatt).sendToTarget();
        }

        //when we have completed a read request we need to place the result
        //in the appropriate placed mapped to the device who provided the data
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == GATT_SUCCESS) {
                ReadResult readRes = new ReadRes(gatt, characteristic);
                bleHandler.obtainMessage(MSG_READ, readRes).sendToTarget(); 
            }
            else{
                Log.e("BlueNet", "Read failed!");
            }
        }

        //when a notification fires, we'll receive the header for message on the
        //appropriate characteristic
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            
            

        }
    };


    /**** **** End of Gatt Client **** ****/

    @Override
    public String ask(String question) {
        return null;
    }
}
