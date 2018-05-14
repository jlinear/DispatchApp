package com.example.marco.bluenet_01;

import android.location.Location;

/**
 * Created by marco on 4/9/2018.
 * Interface for BlueNet
 */

public interface BlueNetInterface {
    int getMyID(); // Returns id of user's device
    int write(String id, String input); // Returns number of bytes written, error code (-1) otherwise
    void regCallback(Runnable functionPointer); // sets function to be called on message received
    void onReceivedMessageListener();
    int[] getNeighbors(int id); // returns array of ids connected to certain device
    Location getLocation(int id); // returns location of id, or (0,0) if id does not exist
}
