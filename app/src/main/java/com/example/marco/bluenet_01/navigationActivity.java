package com.example.marco.bluenet_01;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.marco.bluenet_01.BLE_Comm.BleReader;
import com.example.marco.bluenet_01.BLE_Comm.BleWriter;
import com.example.marco.bluenet_01.BLE_Comm.BlueNet;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.UUID;


public class navigationActivity extends AppCompatActivity
        implements
        mapsFragment.OnFragmentInteractionListener,
        chatFragment.OnFragmentInteractionListener,
        contactFragment.OnFragmentInteractionListener,
        profileFragment.OnFragmentInteractionListener,
        protocolFragment.OnFragmentInteractionListener,
        aboutFragment.OnFragmentInteractionListener,
        NavigationView.OnNavigationItemSelectedListener,
        CompoundButton.OnCheckedChangeListener{

    private FusedLocationProviderClient mFusedLocationClient;
    public BlueNet mBluenet;
    private String myID;



    Fragment mapsFragment = new mapsFragment();

    /**** GATT declarations ****/
    private static final UUID MSG_SERVICE_UUID = UUID
            .fromString("00001869-0000-1000-8000-00805f9b34fb");
    private static final UUID MSG_CHAR_UUID = UUID.
            fromString("00002a09-0000-1000-8000-00805f9b34fb");
    private static final UUID SMSG_CHAR_UUID = UUID.
            fromString("00002a10-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHAR_CONFI_UUID = UUID.
            fromString("00002a08-0000-1000-8000-00805f9b34fb");
    private static final UUID SCLIENT_CHAR_CONFI_UUID = UUID.
            fromString("00002a07-0000-1000-8000-00805f9b34fb");





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.darker_blue)));


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set navigation drawer header
        View headerView = navigationView.getHeaderView(0);
        TextView navUser = (TextView) headerView.findViewById(R.id.userIDNavBar);
        navUser.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("userName", ""));
        TextView navStatus = (TextView) headerView.findViewById(R.id.statusNavBar);
        navStatus.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("statusPref", ""));

        /* From http://www.arjunsk.com/android/how-to-use-fragment-layout-and-scroll-layout-in-android-studio/ */
        //NOTE:  Checks first item in the navigation drawer initially
        navigationView.setCheckedItem(R.id.nav_maps);

        //NOTE:  Open fragment1 initially.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.mainFrame, new mapsFragment());
        ft.commit();

        myID = PreferenceManager.getDefaultSharedPreferences(this).getString("userName", "");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mBluenet = new BlueNet(this, this);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Quit?")
                    .setMessage("Are you sure you'd like to quit Dispatch?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            navigationActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
//        mReader.mBluetoothGatt.disconnect();
//        mWriter.mGattServer.close();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);

        SwitchCompat discoverable = (SwitchCompat) findViewById(R.id.switcher);
        discoverable.setOnCheckedChangeListener(this);
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;

        if (id == R.id.nav_maps) {
            fragment = new mapsFragment();
        } else if (id == R.id.nav_chat) {
            fragment = new chatFragment();
        } else if (id == R.id.nav_contact){
            fragment = new contactFragment();
        } else if (id == R.id.nav_profile) {
            fragment = new profileFragment();
        } else if (id == R.id.nav_protocol) {
            fragment = new protocolFragment();
        } else if (id == R.id.nav_about) {
            //fragment = new aboutFragment();
            //TODO: change the url here to the project website
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
            startActivity(browserIntent);
        }

        //NOTE: Fragment changing code
        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.mainFrame, fragment);
            ft.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;

    }

    @Override
    public void onFragmentInteraction(String title) {
        // NOTE:  Code to replace the toolbar title based current visible fragment
        // will not produce a null pointer exception because we define specific titles, no null possible
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            Log.d("DDD", "haha");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        1);
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
                return;
            }
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
//                                AdvertisementPayload outPayload = new AdvertisementPayload();
//                                outPayload.setUserID(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("userName", ""));//(getIntent().getStringExtra("userName"));
//                                outPayload.setLocation(location);
//                                byte[] out = outPayload.getPayload();
//                                BleBasic.startLeAdvertising(out);

                            } else {
                                throw new RuntimeException("Switch:" + " null location");
                            }
                        }
                    });

//            mAdvPayload.setSrcID(myID);
//            mAdvPayload.setDestID("NULL");
//            mBleBasic.restartLeAdvertising(mAdvPayload.getPayload());
        }else{
//            mBleBasic.stopAdvertising();
        }
    }

    public void distressClick(View view) {
        final EditText editText = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Send Distress Signal?")
                .setMessage("Please enter your distress message: (20 bytes limit)")
                .setView(editText)
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // TODO: send the distress signal with BlueNet
                        // use this to get text from prompt: editText.getText();
//                        mBleBasic.startLeAdvertising(editText.getText().toString().getBytes(StandardCharsets.UTF_8));
                        /**** Test large char read  ****/
//                        mReader.readLargeChar();


                        showToast("Distress signal sent!");
                        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                        drawer.closeDrawer(GravityCompat.START);
                        // hide keyboard
                        ((InputMethodManager) getSystemService(navigationActivity.INPUT_METHOD_SERVICE))
                                .toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showToast(String s){
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
