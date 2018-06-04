package com.example.marco.bluenet_01.BLE_Comm;

import android.location.Location;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Created by jerry on 6/1/18.
 */

public class Adv {
    private String UserID;
    private byte[] payload;
    //private Location location;

    public String getUserID(){
        return UserID;
    }

    public void setUserID(String UserID){
        this.UserID = UserID;
    }

    public byte[] adv_payload(){
        if(UserID != null){
            byte[] byte_userID = UserID.getBytes(StandardCharsets.UTF_8);
            byte[] payload = byte_userID;
        }else{
            throw new RuntimeException("No Valid UserID!");
        }
        return payload;
    }

    public String parse_UserID(byte[] scan_payload){
        String uID;
        if (scan_payload != null){
            uID = new String(scan_payload);
        }else{
            throw new RuntimeException("INVALID SCAN RESULTS!");
        }
        return uID;
    }





//    public byte[] getPayload(){
//
//        byte[] byte_latitude = toByteArray(location.getLatitude());
//        byte[] byte_longitude = toByteArray(location.getLongitude());
//
//        byte[] byte_userID = UserID.getBytes(StandardCharsets.UTF_8);
//
//        // concat more fields when necessary
//        ByteBuffer payload_buffer = ByteBuffer.allocate(byte_latitude.length + byte_longitude.length + byte_userID.length);
//        payload_buffer.put(byte_latitude);
//        payload_buffer.put(byte_longitude);
//        payload_buffer.put(byte_userID);
//
//        payload = payload_buffer.array();
//        return payload;
//    }




//    public Location getLocation(){
//        return location
//    }



//    public void setLocation(Location location){
//        this.location = location;
//    }


//    public Location parseLocation(byte[] scan_payload){
//        Location loc;
//        if (scan_payload.length < 16){
//            throw new RuntimeException("INVALID SCAN RESULTS!");
//        }else{
//            byte[] Lat_byte = Arrays.copyOfRange(scan_payload, 0, 8);
//            byte[] Long_byte = Arrays.copyOfRange(scan_payload, 8, 16);
//            byte[] userID_byte = Arrays.copyOfRange(scan_payload, 16, scan_payload.length );
//            Double Lat = toDouble(Lat_byte);
//            Double Long = toDouble(Long_byte);
//            String userID = new String(userID_byte);
//            loc = new Location(userID);
//            loc.setLatitude(Lat);
//            loc.setLongitude(Long);
//        }
//        return loc;
//    }
//
//
//    public static byte[] toByteArray(double value) {
//        byte[] bytes = new byte[8];
//        ByteBuffer.wrap(bytes).putDouble(value);
//        return bytes;
//    }
//
//    public static double toDouble(byte[] bytes) {
//        return ByteBuffer.wrap(bytes).getDouble();
//    }


}
