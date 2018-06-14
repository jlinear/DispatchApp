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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


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
        mBluenet = new BlueNet(getContext(), getActivity());
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
//            BlueNet mBluenet = getArguments().getParcelable("bluenet");

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        SendButton = view.findViewById(R.id.mapBroadcastButton);
        SendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToast("Halo! Test button, no actual usage.");
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

        //stop location updates when Activity is no longer active
        if (mFusedLocationProviderClient != null) {
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }
    }


    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                lastLocation = location;
                String[] nids = mBluenet.getNeighbors();

                if (0 < nids.length) {
                    Log.d("MapsLog", "length of nids " + nids.length +
                            " 1st neighbor id: "+nids[0] + " myID: " +mBluenet.getMyID());
                }

                // makes sure location is updated in the beginning
                if(!locationFound){
                    // add marker for debugging
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude()))
                            .title(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("userName", ""))
                            .snippet(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("statusPref", ""))
                    );
                    updateLocation(lastLocation);
                    locationFound = true;
                }
            }
        };

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

        mBluenet.setLocation((float)latitude, (float)longitude);


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
