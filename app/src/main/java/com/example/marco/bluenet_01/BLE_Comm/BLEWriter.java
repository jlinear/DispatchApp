package com.example.marco.bluenet_01.BLE_Comm;

import android.os.Handler;

import java.util.concurrent.ConcurrentLinkedQueue;

import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.Writer;
import nd.edu.bluenet_stack.Query;

public class BLEWriter extends LayerBase implements Writer, Query {

    ConcurrentLinkedQueue<AdvertisementPayload> mOutQ = new ConcurrentLinkedQueue<>();
    boolean mStarted = false;
    Handler mHandler = new Handler();
    Runnable mRunnable;

    public BLEWriter() {

        mRunnable = new Runnable() {
            @Override
            public void run() {
                AdvertisementPayload toSend = mOutQ.poll();

                if (null != toSend) {
                    byte [] header = toSend.getHeader();
                    byte [] message = toSend.getMsg();
                    int type = toSend.getMsgType();

                    //Now write to my characteristic
                    //iterate over list of devices and notify
                    byte[] nextHopNeighbor = toSend.getOneHopNeighbor();
                    if (null != nextHopNeighbor) {
                        //only notify specified neighbor
                    }
                    else {
                        //notify all neighbors!
                    }
                }
                mHandler.postDelayed(mRunnable, 30);
            }
        };
    }

    private void setUpGattServer() {

    }

    private void startAdvertising() {

    }


    public int write(AdvertisementPayload advPayload) {
        mOutQ.add(advPayload);
        if (!mStarted) {
            mRunnable.run();
            mStarted = true;
        }

        return 0;
    }
    public int write(String dest, byte[] message) {
        throw new java.lang.UnsupportedOperationException("Not supported.");
    }

    public String ask(String question) {
        String resultString = new String();


        return resultString;
    }
}
