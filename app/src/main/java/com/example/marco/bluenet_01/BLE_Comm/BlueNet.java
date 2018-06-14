package com.example.marco.bluenet_01.BLE_Comm;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

import nd.edu.bluenet_stack.BlueNetIFace;
import nd.edu.bluenet_stack.Result;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.RoutingManager;
import nd.edu.bluenet_stack.GroupManager;
import nd.edu.bluenet_stack.Group;
import nd.edu.bluenet_stack.LocationManager;
import nd.edu.bluenet_stack.MessageLayer;
import nd.edu.bluenet_stack.Query;
import nd.edu.bluenet_stack.RandomString;
import nd.edu.bluenet_stack.Reader;
import nd.edu.bluenet_stack.Writer;
import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.Coordinate;

public class BlueNet implements BlueNetIFace {
    private Result mResultHandler = null;

    private ArrayList<LayerBase> mLayers = new ArrayList<>();

    private RoutingManager mRoute = new RoutingManager();
    private GroupManager mGrp = new GroupManager();
    private LocationManager mLoc = new LocationManager();
    private MessageLayer mMsg = new MessageLayer();
    private BleWriter mBLEW;
    private BleReader mBLER;

    private Query mQuery;
    private RandomString mRandString = new RandomString(4);
    private String mID;


    /**
     * Initialize the ProtocolContainer by add all of the layers to an
     * arraylist for easier management. The order in which they are added
     * is important here because we can use it to distinguish the top of the
     * stack (the part that will return answers to the application and
     * handle writes from the application).
     *
     * <p>Also sets up the query object which is used to handle and direct
     * queries from network layers to the appropriate layer.
     *
     * <p>Assign an alphanumeric BlueNet ID to this device and connect the
     * layers
     *
     */
    public BlueNet (Context context, Activity activity) {

        mBLEW = new BleWriter(context, activity);
        mBLER = new BleReader(context, activity);
        mLayers.add(mRoute);
        mLayers.add(mGrp);
        mLayers.add(mLoc);
        mLayers.add(mMsg);
        mLayers.add(mBLEW);
        mLayers.add(mBLER);

        mID = mRandString.nextString();

        mQuery = new Query() {
            public String ask(String question) {
                final String TAG_Q = "tag";
                final int TAG = 0;
                final int QUERY = 1;
				/*
					Questions will be dispatched to specific submodules from here
					Each question/command must start with a tag word (no periods) followed
					by a period and then the query:
					<tag>.<query>

					each submodule (layer) must at least respond to query('tag') with something
					to identify the layer for dispatching the query

					The top-most module (the one in which this is implemented) can receive
					queries if it catches the tag 'global'

				*/
                String[] parts = question.split("\\.", 2);

                String resultString = new String();

                if (Objects.equals("global", parts[TAG])) {
                    if (Objects.equals("id", parts[QUERY])) {
                        resultString = mID;
                    }
                    else if (Objects.equals("reset id", parts[QUERY])) { //id collision detected so regen
                        mID = mRandString.nextString();
                    }
                    else if (Objects.equals("getNewID", parts[QUERY])) {
                        resultString = mRandString.nextString();
                    }
                    else if (parts[QUERY].contains("setLocation")) { //maybe all other global queries are passed to everyone?
                        for (LayerBase layer: mLayers) {
                            if (layer instanceof Query){
                                resultString = ((Query)layer).ask(parts[QUERY]);
                            }
                        }
                    }
                }
                else {

                    for (LayerBase layer: mLayers) {
                        if (layer instanceof Query){
                            if (Objects.equals(parts[TAG], ((Query)layer).ask(TAG_Q))) {

                                resultString = ((Query)layer).ask(parts[QUERY]);
                            }
                        }
                    }
                }

                return resultString;
            }
        };

        for (LayerBase layer: mLayers) {
            layer.setQueryCB(mQuery);
        }

        connectLayers();
    }

    private void connectLayers() {
        //Connect the layers together

        //the dummy ble layer get AdvertisementPayloads and passes them to
        //the message layer
        mBLER.setReadCB(mMsg);

        //The message layer writes AdvertisementPayloads to the
        //dummy ble layer
        mMsg.setWriteCB(mBLEW);

        //The message layer will hand off messages to this (the top layer) to be printed
        //However, an AdvertisementPayload is passed up then it is sent to LocationManager
        //to handle
        mMsg.setReadCB(mLoc);

        //The location manager passes messages up the stack to the group manager
        mLoc.setReadCB(mGrp);

        //group manager hands to routing manager or all the way to result handler
        mGrp.setReadCB(new Reader() {
            public int read(AdvertisementPayload advPayload) {
                return mRoute.read(advPayload);
            }
            public int read(String src, byte[] message) {
                if (mResultHandler != null) {
                    mResultHandler.provide(src, message);
                }
                return 0;
            }
        });

        //routing manager can only hand 'up' to result handler
        mRoute.setReadCB(new Reader() {
            public int read(AdvertisementPayload advPayload) {
                return -1;
            }
            public int read(String src, byte[] message) {
                if (mResultHandler != null) {
                    mResultHandler.provide(src, message);
                }
                return 0;
            }
        });

        //pass writes down to the message layer
        mRoute.setWriteCB(mMsg);


    }

    //***********************************
    //Interface things to implement
    //***********************************


    public String getMyID() {
        return mID;
    }

    public int write(String destID, String input) {

        int result = ((Writer)mLayers.get(0)).write(destID, input.getBytes(StandardCharsets.UTF_8));

        if (0 == result) {
            result = input.length();
        }

        return result;
    }

    public void regCallback(Result resultHandler) {
        mResultHandler = resultHandler;
    }

    public String[] getNeighbors() {
        String res = mQuery.ask("LocMgr.getNeighbors");

        String[] ids = new String[0];
        if (null != res) {
            ids = res.split("\\s+");
        }

        return ids;
    }

    public void setLocation(float latitude, float longitude) {
        mQuery.ask("global.setLocation " + String.valueOf(latitude) + " " + String.valueOf(longitude));
    }

    public Coordinate getLocation(String id) {
        String queryRes = mQuery.ask("LocMgr.getLocation " + id);
        String[] parts = queryRes.split("\\s+");
        return new Coordinate(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]));
    }

    public Group [] getGroups()	{
        return mGrp.getGroups();
    }

    public void addGroup(String name) {
        mQuery.ask("GrpMgr.addGroup " + name);
    }

    public void addGroup(float lat, float lon, float rad) {
        mQuery.ask("GrpMgr.addGroup " + String.valueOf(lat) + " " + String.valueOf(lon) + " " + String.valueOf(rad));
    }

    public boolean joinGroup(String id) {
        String res = mQuery.ask("GrpMgr.joinGroup " + id);
        return Objects.equals("ok", res);
    }

    public boolean leaveGroup(String id) {
        String res = mQuery.ask("GrpMgr.leaveGroup " + id);
        return Objects.equals("ok", res);
    }
}
