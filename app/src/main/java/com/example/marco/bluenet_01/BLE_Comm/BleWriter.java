package com.example.marco.bluenet_01.BLE_Comm;

import nd.edu.bluenet_stack.AdvertisementPayload;
import nd.edu.bluenet_stack.LayerBase;
import nd.edu.bluenet_stack.LayerIFace;
import nd.edu.bluenet_stack.Query;
import nd.edu.bluenet_stack.Writer;

/**
 * Created by jerry on 6/4/18.
 */

public class BleWriter extends LayerBase
        implements
        Writer,
        Query{

    @Override
    public int write(AdvertisementPayload advPayload) {
        return 0;
    }

    @Override
    public int write(String dest, byte[] message) {
        return 0;
    }

    @Override
    public String ask(String question) {
        return null;
    }
}
