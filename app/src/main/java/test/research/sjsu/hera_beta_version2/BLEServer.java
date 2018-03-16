package test.research.sjsu.hera_beta_version2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;

import static test.research.sjsu.hera_beta_version2.BLEHandler.transmitting;
import static test.research.sjsu.hera_beta_version2.MainActivity.android_id;
import static test.research.sjsu.hera_beta_version2.MainActivity.mBLEHandler;
import static test.research.sjsu.hera_beta_version2.MainActivity.mConnectionSystem;
import static test.research.sjsu.hera_beta_version2.MainActivity.mHera;
import static test.research.sjsu.hera_beta_version2.MainActivity.mMessageSystem;

/**
 * Created by Steven on 3/13/2018.
 */

public class BLEServer {
    private Context sContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private String TAG = "BLEServer";

    BLEServer(BluetoothManager BLEManager, Context systemContext) {
        mBluetoothManager = BLEManager;
        sContext = systemContext;
    }

    public void startServer() {
        BluetoothGattService mBluetoothGattService = new BluetoothGattService(BLEHandler.mServiceUUID, 0);
        BluetoothGattCharacteristic mBluetoothGattCharacteristic = new BluetoothGattCharacteristic(BLEHandler.mCharUUID,BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattCharacteristic mAndroidIDCharacteristic = new BluetoothGattCharacteristic(BLEHandler.mAndroidIDCharUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mBluetoothGattService.addCharacteristic(mAndroidIDCharacteristic);
        mBluetoothGattService.addCharacteristic(mBluetoothGattCharacteristic);
        mBluetoothGattServer = mBluetoothManager.openGattServer(sContext, mBluetoothGattServerCallback);
        mBluetoothGattServer.addService(mBluetoothGattService);
        Log.d(TAG, "GATT Server Initiated");
    }

    public void stopServer() {
        mBluetoothGattServer.close();
    }

    BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        String TAG = "BluetoothGattServerCallBack";
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected to " + device);
            }
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected to" + device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            if (characteristic.getUuid() == BLEHandler.mAndroidIDCharUUID) {
//                Log.d(TAG, "Android ID read request received, sending android ID" + android_id);
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, android_id.getBytes());
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            if (characteristic.getUuid().equals(BLEHandler.mAndroidIDCharUUID)) {
                String neighborAndroidID = new String(value);
                mConnectionSystem.updateConnection(neighborAndroidID, device);
                mConnectionSystem.getConnection(neighborAndroidID).setMyHERAMatrix(mHera.getReachabilityMatrix());
            }

            if (characteristic.getUuid().equals(BLEHandler.mCharUUID)) {
                int dataType = value[0];
                int dataSequence = value[1];
                int isLast = value[2];
                Connection curConnection = mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(device));
                curConnection.writeToCache(Arrays.copyOfRange(value, curConnection.getOverHeadSize(), value.length));
                if (isLast == 0) {
                    if (dataType == ConnectionSystem.DATA_TYPE_MATRIX) {
                        Log.d(TAG, "neighbor HERA matrix received");
                        curConnection.buildNeighborHERAMatrix();
                        curConnection.resetCache();
                        String neighborAndroidID = curConnection.getNeighborAndroidID();
                        mHera.updateDirectHop(neighborAndroidID);
                        mHera.updateTransitiveHops(neighborAndroidID, curConnection.getNeighborHERAMatrix());
                        while(transmitting)
                        {
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG, "Other thread transmitting, waiting");
                        }
                        mMessageSystem.buildToSendMessageQueue(curConnection);
                        if (!curConnection.isToSendQueueEmpty()) {
                            mBLEHandler.sendMessage(curConnection);
                        }
                        else {
                            Log.d(TAG, "No message to send");
                        }
                    } else if (dataType == ConnectionSystem.DATA_TYPE_MESSAGE) {
                        curConnection.buildMessage();
                        curConnection.resetCache();
                    }
                }
            }
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }
    };
}
