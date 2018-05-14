package com.example.marco.bluenet_01;

import android.location.Location;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by marco on 4/9/2018.
 * Implementation for BlueNet
 */

public class BlueNetImplementation implements BlueNetInterface {
    @Override
    public int getMyID() {
        return 0;
    }

    @Override
    public int write(String username, String input) {
        return 0;
    }

    @Override
    public void regCallback(Runnable functionPointer) {

    }

    @Override
    public void onReceivedMessageListener() {

    }

    @Override
    public int[] getNeighbors(int id) {
        return new int[0];
    }

    @Override
    public Location getLocation(int id) {
        return null;
    }
}
