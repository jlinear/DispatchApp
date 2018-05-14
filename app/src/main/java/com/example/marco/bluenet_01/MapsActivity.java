package com.example.marco.bluenet_01;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Calendar;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    LocationManager locationManager;
    String provider;
    Location currentLocation;
    String originalName;
    TextView deviceNameText;
    EditText broadcastInput;
    private BluenetService BlueNet;
    private BluetoothAdapter BA;
    // Temporary listviews to detect changes in bluenet
    ListView messagesList;
    int numMessages;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize BlueNet
        BlueNet = new BluenetService(getApplicationContext(), this);
        BA = BlueNet.getBluetoothAdapter();
        //BlueNet.setReceiver();
        setReceiver();

        // Initialize ListViews
        // TODO: Listviews on side of display which show closest users
        messagesList = new ListView(this);
        numMessages = 0;

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        // Set device name
        deviceNameText = findViewById(R.id.mapDeviceName);
        originalName = this.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("originalName", "");
        deviceNameText.setText(originalName);

        // Identify input area
        broadcastInput = findViewById(R.id.inputMessageText);
        broadcastInputListener();

        // stops keyboard from opening automatically
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // get current location
        try {
            currentLocation = locationManager.getLastKnownLocation(provider);
        }catch (SecurityException e){
            e.printStackTrace();
            showToast("cannot get location, please enable location services.");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // make sure discovery isn't running
        if(BA != null){
            BA.cancelDiscovery();
        }
        // reset bluetooth name
        BA.setName(BlueNet.originalName);
        // unregister broadcast listeners
        //unregisterReceiver(BlueNet.mReceiver);
        unregisterReceiver(mReceiver);
    }

    private void broadcastInputListener(){
        broadcastInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEND) {
                    // hides keyboard after send is pressed
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

                    //mapBroadcastClick(null);
                    return true;
                }
                return false;
            }
        });
    }

    void setReceiver(){

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        this.registerReceiver(mReceiver, filter);

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
                    // If there is a message contained in the device name, show it
                    if(detectedDeviceName.matches(".*_BN_.*")){
                        //plotFoundDevice(detectedDeviceName);
                    }
                }
            }
        }
    };

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(41.705163, -86.234609);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15));

        try {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }catch (SecurityException e){
            e.printStackTrace();
            showToast("Cannot get location, enable location services.");
        }

        try {
            locationManager.requestLocationUpdates(provider, 400, 1, this);
            try {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 17));
                showToast("Location Found!");
            }catch (NullPointerException e){
                // Defaults zoom to Notre Dame
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(41.707005, -86.235346), 17));
                showToast("Please Zoom to your location!");
            }
        }catch (SecurityException e){
            e.printStackTrace();
            showToast("Cannot get location, enable location services.");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            locationManager.requestLocationUpdates(provider, 400, 1, this);
        }catch (SecurityException e){
            e.printStackTrace();
            showToast("Cannot get location, enable location services.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        try{
            locationManager.requestLocationUpdates(provider, 400, 1, this);
            currentLocation = locationManager.getLastKnownLocation(provider);
        }catch (SecurityException e){
            e.printStackTrace();
        }
        updateLocation(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void updateLocation(Location location){
        currentLocation = location;
    }

    /*public void mapFindDevicesClick(View view){
        // when find devices is clicked
        mMap.clear();
        BlueNet.findDevices(10);
        /*messagesList.setAdapter(BlueNet.arrayMessagesAdapter);
        while(BA.isDiscovering()) {
            showToast("looking for messages");
            if (numMessages < BlueNet.messagesList.size()) {
                showToast("there is a new message.");
            }
        }*/
    //}

    // when broadcast is clicked
    /*public void mapBroadcastClick(View view){
        showToast("Broadcasted!");
        // send message
        deviceNameText.setText(BlueNet.sendMessage(broadcastInput.getText().toString(), currentLocation));
        // erases sent input
        broadcastInput.setText("");
    }*/

    // When device is found, it is plotted on a map
    /*private void plotFoundDevice(String device){
        showToast("Device Found!");
        String[] deviceInfo = device.split("_");
        LatLng foundDeviceLocation = new LatLng(Double.parseDouble(deviceInfo[4]), Double.parseDouble(deviceInfo[5]));
        LatLng myLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(myLocation)
                .title("You at " + Calendar.getInstance().getTime())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );
        mMap.addMarker(new MarkerOptions()
                .position(foundDeviceLocation)
                .title(deviceInfo[0])
                .snippet(deviceInfo[2])
        );
        mMap.addPolyline(new PolylineOptions()
                .add(myLocation)
                .add(foundDeviceLocation)
        );
    }*/

    private void showToast(String s){
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

}
