package com.android.bluetoothprinter.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class BluetoothUtil{
    private static final String TAG = "BluetoothUtil";
    private static final UUID  IPOSPRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String IPosPrinter_Address = "00:AA:11:BB:22:CC";

    public static BluetoothAdapter getBluetoothAdapter(){
        return BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothDevice getIposPrinterDevice(BluetoothAdapter mBluetoothAdapter){
        BluetoothDevice IPosPrinter_device = null;
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices){
            if(device.getAddress().equals(IPosPrinter_Address))
            {
                IPosPrinter_device =device;
                break;
            }
        }
        return IPosPrinter_device;
    }

    public static BluetoothSocket getSocket(BluetoothDevice mDevice) throws IOException
    {
        BluetoothSocket socket = mDevice.createRfcommSocketToServiceRecord(IPOSPRINTER_UUID);
        socket.connect();
        return socket;
    }
}