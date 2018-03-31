package test.research.sjsu.hera_beta_version2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.Queue;

import static test.research.sjsu.hera_beta_version2.MainActivity.mMessageSystem;
import static test.research.sjsu.hera_beta_version2.MainActivity.mUiManager;

/**
 * Connection
 * Superclass that handles the BLE connections
 * Attempt to combine the two unidirectional asynchronous BLE connections
 * Created by Steven on 3/13/2018.
 */

class Connection {
    private BluetoothGatt _gatt;
    private BluetoothDevice _device;
    private ByteArrayOutputStream _cache;
    private int _clientMTU;
    private int _Datasize;
    private HERAMatrix _myHERAMatrix;
    private HERAMatrix _neighborHERAMatrix;
    private byte[] toSendPacket;
    private int _totalSegmentCount;
    private Queue<String> _toSendQueue;
    private static String TAG = "Connection";
    private int _connectionOverhead = 3;
    private String neighborAndroidID;
    private long _lastConnectedTime = Integer.MIN_VALUE;

    Connection(String androidID, BluetoothGatt gatt) {
        _gatt = gatt;
        neighborAndroidID = androidID;
        _cache = new ByteArrayOutputStream();
        _toSendQueue = new LinkedList<>();
        _clientMTU = 20;
        _Datasize = _clientMTU - _connectionOverhead;
        _lastConnectedTime = System.currentTimeMillis() / 1000 - HERA.REACH_COOL_DOWN_PERIOD;
    }
    Connection(String androidID, BluetoothDevice device) {
        _device = device;
        neighborAndroidID = androidID;
        _cache = new ByteArrayOutputStream();
        _toSendQueue = new LinkedList<>();
    }

    int getOverHeadSize() {
        return _connectionOverhead;
    }

    String getNeighborAndroidID() {
        return neighborAndroidID;
    }

    void writeToCache(byte[] input) {
        try {
            _cache.write(input);
        } catch (IOException e) {
            e.fillInStackTrace();
        }
    }

    void buildNeighborHERAMatrix() {
        ObjectInputStream input;
        try {
            byte[] cacheByteArr = _cache.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(cacheByteArr);
            input = new ObjectInputStream(inputStream);
            _neighborHERAMatrix = (HERAMatrix) input.readObject();
//            Log.d(TAG, "neighbor HERA matrix received: " + _neighborHERAMatrix.toString());
        } catch (Exception e) {
            Log.e(TAG,"Reconstruct map exception" + e.fillInStackTrace());
        }
    }

    HERAMatrix getNeighborHERAMatrix() {
        return _neighborHERAMatrix;
    }

    void resetCache() {
        _cache = new ByteArrayOutputStream();
    }

    void setDevice(BluetoothDevice device) {
        _device = device;
    }

    void setGatt(BluetoothGatt gatt) {
        _gatt = gatt;
    }

    BluetoothGatt getGatt() {
        return _gatt;
    }


    private byte[] getCacheByteArray() {
        return _cache.toByteArray();
    }

    void buildMessage() {
        mMessageSystem.putMessage(getCacheByteArray());
//        Log.d(TAG, "Message built: " + new String(getCacheByteArray()));
//        Log.d(TAG, "Message System size: " + mMessageSystem.getMessageSystemSize());
        mUiManager.updateMessageSystemUI();
    }

    void setClientMTU(int mtu) {
        this._clientMTU = 300;
        this._Datasize = _clientMTU - _connectionOverhead;
    }


    int getDatasize() {
        return _Datasize;
    }


    void setMyHERAMatrix (HERAMatrix map)  {
        _myHERAMatrix = map;
    }
    void flattenMyHeraMatrix() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(_myHERAMatrix);
            toSendPacket = bos.toByteArray();
            Log.d(TAG, "Successfully converted Map to Byte Array, size : " + toSendPacket.length);
            oos.close();
            _totalSegmentCount = toSendPacket.length / _Datasize + (toSendPacket.length % _Datasize == 0 ? 0 : 1);
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }
    void setCurrentToSendPacket(byte[] data) {
        toSendPacket = data;
        _totalSegmentCount = +toSendPacket.length / _Datasize + (toSendPacket.length % _Datasize == 0 ? 0 : 1);
    }
    HERAMatrix getMyHERAMatrix() {
//        Log.d(TAG, "getMyHERAMatrix: " + _myHERAMatrix);
        return _myHERAMatrix;
    }

    int getTotalSegmentCount() {
        return _totalSegmentCount;
    }

    byte[] getToSendPacket() {
        return toSendPacket;
    }

    Queue<String> getToSendQueue() {
        return _toSendQueue;
    }

    String getOneToSendDestination() {
        return _toSendQueue.peek();
    }

    void toSendDestinationUpdate() {
        _toSendQueue.remove();
    }
    boolean isToSendQueueEmpty() {
        return _toSendQueue.isEmpty();
    }

    void pushToSendQueue(String dest) {
        _toSendQueue.add(dest);
    }

    long getLastConnectedTimeDiff() {
        Log.d(TAG, "last connected time: "+ ((System.currentTimeMillis()/ 1000) - _lastConnectedTime));
        return (System.currentTimeMillis() / 1000) - _lastConnectedTime;
    }

    void updateLastConnectedTime() {
        _lastConnectedTime = System.currentTimeMillis() / 1000;
    }
}
