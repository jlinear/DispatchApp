package com.example.marco.bluenet_01.BLE_Comm;

import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.Query;
import nd.edu.bluenet_stack.Reader;

/**
 * Created by jerry on 6/4/18.
 */

public class BleReader extends LayerBase
        implements
        Reader,
        Query{

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
