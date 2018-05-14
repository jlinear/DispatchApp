package com.example.marco.bluenet_01;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class navigationActivity extends AppCompatActivity
        implements
        mapsFragment.OnFragmentInteractionListener,
        chatFragment.OnFragmentInteractionListener,
        profileFragment.OnFragmentInteractionListener,
        protocolFragment.OnFragmentInteractionListener,
        aboutFragment.OnFragmentInteractionListener,
        NavigationView.OnNavigationItemSelectedListener {

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
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
        } else if (id == R.id.nav_profile) {
            fragment = new profileFragment();
        } else if (id == R.id.nav_protocol) {
            fragment = new protocolFragment();
        } else if (id == R.id.nav_about) {
            fragment = new aboutFragment();
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

    public void distressClick(View view) {
        final EditText editText = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Send Distress Signal?")
                .setMessage("Please enter your distress message:")
                .setView(editText)
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // TODO: send the distress signal with BlueNet
                        // use this to get text from prompt: editText.getText();
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
