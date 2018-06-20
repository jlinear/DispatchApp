package com.example.marco.bluenet_01;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.marco.bluenet_01.BLE_Comm.BlueNet;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import nd.edu.bluenet_stack.Result;


/**
 * Created by jerry on 6/6/18.
 */

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link contactFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link contactFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class contactFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    TextView headerText;
    String headerTextString;
    BlueNet mBluenet;

    /**** DEBUG use ****/
    private ListView mListPeople, mListGroup;

    private String [] data1 ={"Hiren", "Pratik", "Dhruv", "Narendra", "Piyush", "Priyank"};
    private String [] data2 ={"TestGroup1", "TestGroup2", "TestGroup3"};

    public contactFragment(){

    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment contactFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static contactFragment newInstance(String param1, String param2) {
        contactFragment fragment = new contactFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        EventBus.getDefault().register(this);
        mBluenet = EventBus.getDefault().getStickyEvent(BlueNet.class);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_about, container, false);
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        mListPeople = view.findViewById(R.id.list_people);
        mListGroup = view.findViewById(R.id.list_group);

        final String [] uids = mBluenet.getNeighbors();
        List<String> uidList = Arrays.asList(uids);
        final ListAdapter PeopleListAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, uidList);
        ListAdapter GroupListAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, data2);

        mListPeople.setAdapter(PeopleListAdapter);
        mListGroup.setAdapter(GroupListAdapter);

        mListPeople.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO: get the Bluenet ID and pass to chat fragment
                String id = uids[i];
                Fragment fg= new chatFragment();
                Bundle bundle = new Bundle();
                bundle.putString("chattingName", id);
                fg.setArguments(bundle);

                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.mainFrame, fg);
                ft.commit();
            }
        });
        mListGroup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO: get group ID and pass to chat fragment
                String id = data2[i];
                Fragment fg= new chatFragment();
                Bundle bundle = new Bundle();
                bundle.putString("chattingName", id);
                fg.setArguments(bundle);

                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.mainFrame, fg);
                ft.commit();
            }
        });

        ListUtils.setDynamicHeight(mListPeople);
        ListUtils.setDynamicHeight(mListGroup);

        FloatingActionButton mFab_add_contact = view.findViewById(R.id.add_contact);
        FloatingActionButton mFab_add_people = view.findViewById(R.id.add_contact_people);
        FloatingActionButton mFab_add_group = view.findViewById(R.id.add_contact_group);
        final LinearLayout mPeopleLayout = view.findViewById(R.id.layout_add_people);
        mPeopleLayout.setVisibility(View.GONE);
        final LinearLayout mGroupLayout = view.findViewById(R.id.layout_add_group);
        mGroupLayout.setVisibility(View.GONE);

        mFab_add_contact.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(mPeopleLayout.getVisibility() == View.VISIBLE && mGroupLayout.getVisibility() == View.VISIBLE){
                    mPeopleLayout.setVisibility(View.GONE);
                    mGroupLayout.setVisibility(View.GONE);
                }else{
                    mPeopleLayout.setVisibility(View.VISIBLE);
                    mGroupLayout.setVisibility(View.VISIBLE);

                }
            }
        });

        mFab_add_people.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText editText = new EditText(getContext());
                new AlertDialog.Builder(getContext())
                        .setTitle("Add new contact")
                        .setMessage("Search by user ID:")
                        .setView(editText)
                        .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //TODO: pass the userID to bluenet for ID search
                                String new_id = editText.getText().toString();
//                                uidList.add(new_id);
//                                PeopleListAdapter.notify();
                            }
                        })
                        .setNegativeButton("Cancel",null)
                        .show();
            }
        });

        mFab_add_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText editText = new EditText(getContext());
                new AlertDialog.Builder(getContext())
                        .setTitle("Join new group by name")
                        .setMessage("Search by group name")
                        .setView(editText)
                        .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //TODO: pass the group name to bluenet for group search
                            }
                        })
                        .setNegativeButton("Cancel",null)
                        .show();
            }
        });


        mBluenet.regCallback(new Result() {
            @Override
            public int provide(String src, byte[] data) {
                final String rec_msg = new String(data, StandardCharsets.UTF_8);
//                showToast(src + ": " + rec_msg);
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
            mListener.onFragmentInteraction("Contacts");
        }

        // Here we will can create click listners etc for all the gui elements on the fragment.
        // For eg: Button btn1= (Button) view.findViewById(R.id.frag1_btn1);
        // btn1.setOnclickListener(...

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    /*public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }*/

    @Subscribe
    public void RecBluenet(BlueNet mbluenet){
        Log.d("Contact","num of neighbors "+mbluenet.getNeighbors().length);
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
    }


//    public ListPeopleClickListener = new AdapterView.OnItemClickListener(){
//
//    }

    public static class ListUtils {
        public static void setDynamicHeight(ListView mListView) {
            ListAdapter mListAdapter = mListView.getAdapter();
            if (mListAdapter == null) {
                // when adapter is null
                return;
            }
            int height = 0;
            int desiredWidth = View.MeasureSpec.makeMeasureSpec(mListView.getWidth(), View.MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < mListAdapter.getCount(); i++) {
                View listItem = mListAdapter.getView(i, null, mListView);
                listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                height += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = mListView.getLayoutParams();
            params.height = height + (mListView.getDividerHeight() * (mListAdapter.getCount() - 1));
            mListView.setLayoutParams(params);
            mListView.requestLayout();
        }
    }

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
