package com.example.marco.bluenet_01;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.marco.bluenet_01.BLE_Comm.BlueNet;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import co.intentservice.chatui.ChatView;
import co.intentservice.chatui.models.ChatMessage;
import nd.edu.bluenet_stack.Result;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link chatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link chatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class chatFragment extends Fragment {
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
    ChatView chatView;
    String chattingWith;
    String firstMsg;
    Long firstMsgTime;
    BlueNet mBluenet;

    public chatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment chatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static chatFragment newInstance(String param1, String param2) {
        chatFragment fragment = new chatFragment();
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
//        Log.d("CHAT","xbluenet got!"+ mBluenet.getNeighbors().length);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_chat, container, false);
        View view = inflater.inflate(R.layout.fragment_chat, container, false);


        // Here we will can create click listners etc for all the gui elements on the fragment.
        // For eg: Button btn1= (Button) view.findViewById(R.id.frag1_btn1);
        // btn1.setOnclickListener(...

        headerText = view.findViewById(R.id.chat_header);
        checkBundle();

        // NOTE : We are calling the onFragmentInteraction() declared in the MainActivity
        // ie we are sending "Fragment 1" as title parameter when fragment1 is activated

        // Send message through bluenet and receive on device
        chatView = view.findViewById(R.id.chat_view);
        chatView.setOnSentMessageListener(new ChatView.OnSentMessageListener() {
            @Override
            public boolean sendMessage(ChatMessage chatMessage) {
                // TODO: Implement write below
                mBluenet.write(chattingWith,chatMessage.getMessage());

                return true; // returns true when it should put message on screen, false if it shouldn't
            }
        });

        // this is how you add messages to the screen
//        chatView.addMessage(new ChatMessage("test static default message",System.currentTimeMillis(), ChatMessage.Type.RECEIVED));
        if (firstMsg != null && firstMsgTime != 0) {
            chatView.addMessage(new ChatMessage(firstMsg, firstMsgTime, ChatMessage.Type.RECEIVED));
        }

        mBluenet.regCallback(new Result() {
            @Override
            public int provide(String src, byte[] data) {
                String rec_msg = new String(data, StandardCharsets.UTF_8);
                chatView.addMessage(new ChatMessage(rec_msg, System.currentTimeMillis(), ChatMessage.Type.RECEIVED));

                return 0;
            }
        });


        return view;
    }

    @Subscribe
    public void RecBluenet(BlueNet mbluenet){
        Log.d("CHAT","num of neighbors "+mbluenet.getNeighbors().length);
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

    private void checkBundle(){
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            chattingWith = bundle.getString("chattingName", null);
            firstMsg = bundle.getString("FirstMsg", null);
            firstMsgTime = bundle.getLong("FirstMsgTime", 0);
            if(chattingWith != null){
                headerText.setText(null);
                mListener.onFragmentInteraction("Chat with: " + chattingWith);
            }else{
                mListener.onFragmentInteraction("Dispatch Chat");
            }

//            headerTextString = "Chatting With: " + chattingWith;
//            headerText.setTextColor(getResources().getColor(R.color.black));
//            headerText.setText(headerTextString);
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
        // TODO: Update argument type and name
        void onFragmentInteraction(String title);
    }
}
