package com.example.marco.bluenet_01;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class Chat extends Activity {

    // Global declarations
    private BluenetService BlueNet;
    private BluetoothAdapter BA;
    public ListView devicesList;
    public ListView messagesList;
    private TextView device;
    private NumberPicker np;
    TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        device = findViewById(R.id.deviceName);
        message = findViewById(R.id.typeMessage);
        devicesList = findViewById(R.id.devicesListView);
        messagesList = findViewById(R.id.messagesListView);

        BlueNet = new BluenetService(getApplicationContext(), this);
        np = findViewById(R.id.discoverTime);
        np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        np.setMinValue(1);
        np.setMaxValue(120);

        // Use bluetooth adapter from BluenetService Class
        BA = BlueNet.getBluetoothAdapter();
        device.setText(BlueNet.originalName);

        BlueNet.setReceiver();

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        // make sure discovery isn't running
        if(BA != null){
            BA.cancelDiscovery();
        }
        // reset bluetooth name
        BA.setName(BlueNet.originalName);
        // unregister broadcast listeners
        unregisterReceiver(BlueNet.mReceiver);
    }

    // makes discoverable and finds devices on click
    public void findDevicesClick(View view){
        BlueNet.findDevices(np.getValue());
        devicesList.setAdapter(BlueNet.arrayDevicesAdapter);
        messagesList.setAdapter(BlueNet.arrayMessagesAdapter);
    }

    // sends message on click
    public void sendClick(View view){
        String text = message.getText().toString();
        device.setText(BlueNet.sendMessage(text));
    }

    // resets to original name without message
    public void resetClick(View view){
        BA.setName(BlueNet.originalName);
        BlueNet.makeDiscoverable(1);
        device.setText(BlueNet.originalName);
    }

    // TODO: get lcoation from user and send it
    public void locationClick(View veiw){
    //    sendMessage("COORDINATES GO HERE");
    }

}
