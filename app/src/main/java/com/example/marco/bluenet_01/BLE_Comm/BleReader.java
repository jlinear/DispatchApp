package com.example.marco.bluenet_01.BLE_Comm;

import android.app.Activity;
import android.content.Context;

import com.example.marco.bluenet_01.BuildConfig;

import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.Query;
import nd.edu.bluenet_stack.Reader;

/**
 * Created by jerry on 6/4/18.
 * Scanning and as GATT client
 */

public class BleReader extends LayerBase
        implements
        Reader,
        Query{

    public Context context;
    public Activity activity;
    String originalName;

    public BleReader(Context context, Activity activity){
        this.context = context.getApplicationContext();
        this.activity = activity.getParent();
        this.originalName = activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("originalName", "");
    }

    @Override
    public int read(String src, byte[] message) {
        return 0;
    }

    @Override
    public int read(AdvertisementPayload advPayload) {
        return 0;
    }

    @Override
    public String ask(String question) {
        return null;
    }
}
