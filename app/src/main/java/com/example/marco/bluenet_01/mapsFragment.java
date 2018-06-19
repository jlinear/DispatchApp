package com.example.marco.bluenet_01;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.marco.bluenet_01.BLE_Comm.BlueNet;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import nd.edu.bluenet_stack.Result;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link mapsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link mapsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class mapsFragment extends Fragment implements OnMapReadyCallback  {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    FusedLocationProviderClient mFusedLocationProviderClient;
    Location lastLocation;
    LocationRequest mLocationRequest;
    boolean locationFound = false;
    SupportMapFragment mapFragment;

    Button SendButton;
    BlueNet mBluenet;
    Set<String> neighbor_ids;



    public mapsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment mapsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static mapsFragment newInstance(String param1, String param2) {
        mapsFragment fragment = new mapsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mBluenet = new BlueNet(getContext(), getActivity());
        neighbor_ids = new HashSet<String>();
        EventBus.getDefault().register(this);
        mBluenet = EventBus.getDefault().getStickyEvent(BlueNet.class);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        final EditText edittext = view.findViewById(R.id.inputMessageText);
        SendButton = view.findViewById(R.id.mapBroadcastButton);
        SendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String out_msg = edittext.getText().toString();
                for(int i = 0; i< mBluenet.getNeighbors().length; i++){
                    mBluenet.write(mBluenet.getNeighbors()[i], out_msg);
                }
                showToast("Your msg has been broadcast!");
                edittext.setText("");
            }
        });

        mBluenet.regCallback(new Result() {
            @Override
            public int provide(String src, byte[] data) {
                final String rec_msg = new String(data, StandardCharsets.UTF_8);
                showToast(src + ": " + rec_msg);
                final String src_id = src;
//                EventBus.getDefault().post(mBluenet);

                new AlertDialog.Builder(getContext())
                        .setTitle("New Message Received!")
                        .setMessage("You have a new message from: "+src)
                        .setPositiveButton("Read", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i){
                                chatFragment frag = new chatFragment();
                                Bundle bundle = new Bundle();
                                bundle.putString("chattingName", src_id);
                                bundle.putString("FirstMsg",rec_msg);
                                bundle.putLong("FirstMsgTime",System.currentTimeMillis());
                                frag.setArguments(bundle);

                                //NOTE: Fragment changing code
                                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                                ft.replace(R.id.mainFrame, frag);
                                NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
                                navigationView.setCheckedItem(R.id.nav_chat);
                                ft.commit();
                            }
                        })
                        .setNegativeButton("Discard", null)
                        .show();

                return 0;
            }
        });

        // NOTE : We are calling the onFragmentInteraction() declared in the MainActivity
        // ie we are sending "Fragment 1" as title parameter when fragment1 is activated
        if (mListener != null) {
            mListener.onFragmentInteraction("Dispatch Maps");
        }

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if(mapFragment == null){
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            mapFragment = SupportMapFragment.newInstance();
            ft.replace(R.id.map, mapFragment).commit();
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        mapFragment.getMapAsync(this);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);



        // Here we will can create click listners etc for all the gui elements on the fragment.
        // For eg: Button btn1= (Button) view.findViewById(R.id.frag1_btn1);
        // btn1.setOnclickListener(...

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //SendButton.findViewById(R.id.mapBroadcastButton);

        return view;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onBlueNet(BlueNet xBluenet) {
        Log.d("EVENTBUS","bluenet posted!");
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        EventBus.getDefault().unregister(this);
        neighbor_ids.clear();

        //stop location updates when Activity is no longer active
        if (mFusedLocationProviderClient != null) {
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }
    }


    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.i("MapsActivity", "My Location: " + location.getLatitude() + " " + location.getLongitude());
                mBluenet.setLocation((float)location.getLatitude(), (float)location.getLongitude());
                lastLocation = location;
                String[] nids = mBluenet.getNeighbors();

                if (0 < nids.length) {
                    Log.d("MapsLog", "length of nids " + nids.length +
                            " 1st neighbor id: "+nids[0] + " myID: " +mBluenet.getMyID());
                    Log.d("LocLog", "lat: " + mBluenet.getLocation(nids[0]).mLatitude +
                            " lng: " + mBluenet.getLocation(nids[0]).mLongitude);
                }else{
                    showToast("Searching for neighbors, please wait...");
                }

//                mMap.clear();
                if (0 < nids.length){
                for (int i = 0; i< nids.length; i++){
                    if(!neighbor_ids.contains(nids[i])){
                        neighbor_ids.add(nids[i]);
                        float lat = mBluenet.getLocation(nids[0]).mLatitude;
                        float lng = mBluenet.getLocation(nids[0]).mLongitude;
                        Random ran = new Random();
                        float marker_color = 330 * ran.nextFloat();
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(lat,lng))
                                .title(nids[i])
                                .icon(BitmapDescriptorFactory.defaultMarker(marker_color))
                        );
                    }

//                    neighbor_ids.add(nids[i]);
//                    float lat = mBluenet.getLocation(nids[0]).mLatitude;
//                    float lng = mBluenet.getLocation(nids[0]).mLongitude;
//                    Random ran = new Random();
//                    float marker_color = 330 * ran.nextFloat();
//                    mMap.addMarker(new MarkerOptions()
//                            .position(new LatLng(lat,lng))
//                            .title(nids[i])
//                            .icon(BitmapDescriptorFactory.defaultMarker(marker_color))
//                    );
                }}

                // makes sure location is updated in the beginning
                if(!locationFound){
                    // add self marker for debugging
//                    mMap.addMarker(new MarkerOptions()
//                            .position(new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude()))
//                            .title(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("userName", ""))
//                            .snippet(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("statusPref", ""))
//                    );
                    updateLocation(lastLocation);
                    locationFound = true;
                }
            }
        }
    };


    private GoogleMap mMap;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); // 5 second interval
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        try{
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }catch (SecurityException e){
            e.printStackTrace();
            showToast("mFusedLocationProviderClient failed");
        }
        markerTitleClick();
        SetUpLongClick();
    }

    private void showToast(String s){
        Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
    }

    void updateLocation(Location location){
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        LatLng currentLatLng = new LatLng(latitude, longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
    }

    // Switch to Chat view when marker note is clicked
    void markerTitleClick(){
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                // Add name of user to chatView's header
                chatFragment frag = new chatFragment();
                Bundle bundle = new Bundle();
                bundle.putString("chattingName", marker.getTitle());
                frag.setArguments(bundle);

                //NOTE: Fragment changing code
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.mainFrame, frag);
                NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
                navigationView.setCheckedItem(R.id.nav_chat);
                ft.commit();
            }
        });
    }

    public void SetUpLongClick(){
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {
                double lat = latLng.latitude;
                double lng = latLng.longitude;
                Log.d(this.getClass().getSimpleName(), "Long click on "+ latLng);
                final EditText editText = new EditText(getContext());
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                new AlertDialog.Builder(getContext())
                        .setTitle("Set up your range of interests")
                        .setMessage("Enter the radius (miles): ")
                        .setView(editText)
                        .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //TODO: pass the location and radius for group creation
                                //TODO: add the group center/radius to the contact

                                try{
                                    double radius = Double.parseDouble(editText.getText().toString());
                                    CircleOptions circleOptions = new CircleOptions()
                                            .center(latLng)
                                            .radius(radius * 1609.344) //the accepted unit is meters
                                            .strokeColor(0xffff0000)
                                            .strokeWidth(2);
                                    Circle mCircle = mMap.addCircle(circleOptions);
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton("Cancel",null)
                        .show();


            }
        });
    }





//    public void mapBroadcastClick(View view){
//        switch (view.getId()){
//        }
//        showToast("Halo!");
//    }



    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String title);
    }
}
