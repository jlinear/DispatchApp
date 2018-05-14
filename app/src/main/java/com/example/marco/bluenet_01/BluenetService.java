package com.example.marco.bluenet_01;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marco on 3/13/2018.
 */

class BluenetService {

    // Globals
    private BluetoothAdapter BA;
    private Context context;
    private Activity activity;
    String originalName;
    private ArrayList<String> discoveredList = new ArrayList<String>();
    ArrayList<String> messagesList = new ArrayList<>();
    ArrayAdapter<String> arrayDevicesAdapter;
    ArrayAdapter<String> arrayMessagesAdapter;

    BluenetService(Context context, Activity activity){
        BA = BluetoothAdapter.getDefaultAdapter();
        this.context = context;
        this.activity = activity;
        this.originalName = activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("originalName", "");
    }

    BluetoothAdapter getBluetoothAdapter(){
        return BA;
    }

    void setReceiver(){

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        activity.registerReceiver(mReceiver, filter);

    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
                showToast("discovery begin");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
                showToast("discovery done");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice detectedDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(detectedDevice.getName() != null) {
                    String detectedDeviceName = detectedDevice.getName();
                    discoveredList.add(detectedDeviceName);
                    arrayDevicesAdapter.notifyDataSetChanged();
                    // If there is a message contained in the device name, show it
                    if(detectedDeviceName.matches(".*_BN_.*")){
                        updateRecievedMessage(detectedDeviceName);
                    }
                }
            }
        }
    };

    // makes discoverable and finds devices on click
    void findDevices(int time) {
        makeDiscoverable(time);
        discoveredList.clear();
        arrayDevicesAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, discoveredList);
        arrayMessagesAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, messagesList);
        BA.startDiscovery();
    }
    void makeDiscoverable(int time){
        showToast("asking for permission");
        Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        // change this value for longer discovery time, or 0 for indefinite
        // if indefinite, need to add another intent to ondestroy for 1 second
        discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, time);
        activity.startActivity(discoverIntent);
    }

    // Returns newName to change device name
    String sendMessage(String message){
        String newName = originalName + "_BN_" + message;
        makeDiscoverable(10);
        BA.setName(newName);

        // access message with text[2]
        String[] text = newName.split("_");
        showToast("New device name: "+ newName);
        showToast("Message: " + text[2]);
        return newName;
    }

    // Returns newName to change device name, includes location in name
    String sendMessage(String message, Location location){
        // Location: _LOC_LATITUDE_LONGITUDE
        String newName = originalName + "_BN_" + message + "_LOC_" + location.getLatitude() + "_" + location.getLongitude();
        makeDiscoverable(10);
        BA.setName(newName);

        // access message with text[2]
        String[] text = newName.split("_");
        showToast("New device name: "+ newName);
        showToast("Message: " + text[2]);
        showToast("Location: " + text[4] + "," + text[5]);
        return newName;
    }

    private void updateRecievedMessage(String deviceName){
        String [] text = deviceName.split("_");
        messagesList.add(text[2] + ", From " + text[0]);
        arrayMessagesAdapter.notifyDataSetChanged();
    }

    private void showToast(String s){
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

}
