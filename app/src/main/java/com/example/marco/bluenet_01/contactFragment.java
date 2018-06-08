package com.example.marco.bluenet_01;


import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;



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

    /**** DEBUG use ****/
    private ListView mListPeople, mListGroup;

    private String [] data1 ={"Hiren", "Pratik", "Dhruv", "Narendra", "Piyush", "Priyank"};
    private String [] data2 ={"Kirit", "Miral", "Bhushan", "Jiten", "Ajay", "Kamlesh"};

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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_about, container, false);
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        mListPeople = view.findViewById(R.id.list_people);
        mListGroup = view.findViewById(R.id.list_group);

        mListPeople.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, data1));
        mListGroup.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, data2));

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
                //TODO: popping up a dialog box for searching user and add
            }
        });

        mFab_add_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: dialog box for join group
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
    }

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
