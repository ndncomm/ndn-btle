package com.lannbox.rfduinotest;

import android.os.Message;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.ElementListener;

import java.nio.ByteBuffer;

/**
 * Created by Abhishek on 9/27/15.
 */
public class BTLeConsumer extends Thread implements OnData, OnTimeout {

    private static final String TAG = "BTLeConsumer";
    private long startTime;

    public BTLeConsumer() {
    }

    public void onData(Interest interest, Data data) {
        ++callbackCount_;

        Log.i(TAG, "Got data packet with name " + data.getName().toUri());
        long elapsedTime = System.currentTimeMillis() - this.startTime;
        String name = data.getName().toUri();
        String pingTarget = name.substring(0, name.lastIndexOf("/"));
        String contentStr = pingTarget + ": " + String.valueOf(elapsedTime) + " ms";
        Log.i(TAG, "Content " + contentStr);

        // Send a result to Screen
        Message msg = new Message();
        msg.what = 200; // Result Code ex) Success code: 200 , Fail Code:
        // 400 ...
        msg.obj = contentStr; // Result Object

    }

    public void onTimeout(Interest interest) {
        ++callbackCount_;
        Log.i(TAG, "Time out for interest " + interest.getName().toUri());
    }

    public int callbackCount_ = 0;

    @Override
    public void run() {

        try {
            BTLeForwarder forwarder = new BTLeForwarder();
            Face face = new Face(new BTLeForwarderTransport(),
                    new BTLeForwarderTransport.ConnectionInfo(forwarder));
            Interest testInterest = new Interest(new Name("000/3"));
            face.expressInterest(testInterest, this, this);
            ByteBuffer rfDuinoInterest = testInterest.wireEncode().buf();
            Log.i(TAG, "Created interest" + rfDuinoInterest.toString());
        } catch (Exception e) {
            Log.i(TAG, "exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
