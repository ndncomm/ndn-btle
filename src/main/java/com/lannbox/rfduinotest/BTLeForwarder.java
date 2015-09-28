package com.lannbox.rfduinotest;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.ElementListener;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Abhishek on 9/27/15.
 */
public class BTLeForwarder extends Service {

    private static final String TAG = "BTLeForwarder";

    private long startTime;
    private int state;
    private BluetoothDevice bluetoothDevice;
    private RFduinoService rfduinoService;
    private final IBinder mBinder = new LocalBinder();

    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {
                Log.i(TAG, "RFDuino service connection successful");
                if (rfduinoService.connect(bluetoothDevice.getAddress())) {
                    upgradeState(STATE_CONNECTING);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfduinoService = null;
            downgradeState(STATE_DISCONNECTED);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return(super.onUnbind(intent));
    }

    public Service getRfduinoService() {
        return rfduinoService;
    }
    public void close() {
        if(rfduinoService == null)
            return;
        rfduinoService.close();
    }

    private void upgradeState(int newState) {
        if (newState > state) {
            updateState(newState);
        }
    }

    private void downgradeState(int newState) {
        if (newState < state) {
            updateState(newState);
        }
    }

    private void updateState(int newState) {
        state = newState;
    }

    public boolean startUp() {
        startTime = System.currentTimeMillis();
        Intent rfduinoIntent = new Intent(BTLeForwarder.this, RFduinoService.class);
        if(!bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE))
            return false;
        return true;
    }

    public void
    sendAndReceive(ByteBuffer buffer, ElementListener elementListener)
            throws IOException
    {
        final int tlvInterestType = 5;
        if (buffer.get(0) != tlvInterestType)
            throw new IOException
                    ("BTLeForwarder.sendAndReceive: Input buffer is not an Interest");

        Interest interest = new Interest();
        try {
            interest.wireDecode(buffer);
        } catch (EncodingException ex) {
            throw new IOException
                    ("BTLeForwarder.sendAndReceive: Error decoding the input buffer as an Interest: " +
                            ex.getMessage());
        }

        ByteBuffer arduinoId = interest.getName().get(0).getValue().buf();

        // TODO: Use arduinoId to find the FIB entry for the Arduino.

        // TODO: Send the input buffer to the Arduino and receive fragments.
        // byte[] sendBuffer = null;
        // buffer.get(sendBuffer, 0, buffer.capacity());
        rfduinoService.send(new Blob(buffer, true).getImmutableArray());
        ByteBuffer receiveBuffer = null;
        if(receiveBuffer != null) {
            Log.i(TAG, "Receive Buffer: " + receiveBuffer.array());
        }
        /* try {
            // TODO: Re-assemble the fragments into the receiveBuffer, which we assume
            // is a Data packet from the Arduino.
            // elementListener.onReceivedElement(receiveBuffer);
            Log.i(TAG, "Receive Buffer: " + receiveBuffer.toString());
        } catch (EncodingException ex) {
            throw new IOException
                    ("BTLeForwarder.sendAndReceive: Error in onReceivedElement: " +
                            ex.getMessage());
        } */
    }

    public class LocalBinder extends Binder {
        BTLeForwarder getService() {
            return BTLeForwarder.this;
        }
    }
}
