package com.example.marco.bluenet_01;

import nd.edu.bluenet_stack.BlueNetIFace;
import nd.edu.bluenet_stack.Group;
import nd.edu.bluenet_stack.Result;

/**
 * Created by jerry on 6/4/18.
 */

public class BlueNet implements BlueNetIFace {


    @Override
    public String getMyID() {
        return null;
    }

    @Override
    public int write(String destID, String input) {
        return 0;
    }

    @Override
    public void regCallback(Result resultHandler) {

    }

    @Override
    public String[] getNeighbors() {
        return new String[0];
    }

    @Override
    public String getLocation(String id) {
        return null;
    }

    @Override
    public Group[] getGroups() {
        return new Group[0];
    }

    @Override
    public void addGroup(String name) {

    }

    @Override
    public void addGroup(float lat, float lon, float rad) {

    }

    @Override
    public boolean joinGroup(String id) {
        return false;
    }

    @Override
    public boolean leaveGroup(String id) {
        return false;
    }
}
