package test.research.sjsu.hera_beta_version2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.UUID;

import static test.research.sjsu.hera_beta_version2.BLEHandler.BeanScratchFirstCharUUID;
import static test.research.sjsu.hera_beta_version2.BLEHandler.BeanScratchServiceUUID;
import static test.research.sjsu.hera_beta_version2.BLEHandler.connecting;
import static test.research.sjsu.hera_beta_version2.BLEHandler.connectionStatus;
import static test.research.sjsu.hera_beta_version2.BLEHandler.mAndroidIDCharUUID;
import static test.research.sjsu.hera_beta_version2.BLEHandler.mCharUUID;
import static test.research.sjsu.hera_beta_version2.BLEHandler.mServiceUUID;
import static test.research.sjsu.hera_beta_version2.MainActivity.android_id;
import static test.research.sjsu.hera_beta_version2.MainActivity.mBLEHandler;
import static test.research.sjsu.hera_beta_version2.MainActivity.mConnectionSystem;
import static test.research.sjsu.hera_beta_version2.MainActivity.mHera;
import static test.research.sjsu.hera_beta_version2.MainActivity.mMessageSystem;


/**
 * Created by Steven on 3/13/2018.
 */

public class BLEClient {

    private Context sContext;
    private String TAG = "BLEClient";
    BLEClient(Context systemContext) {
        sContext = systemContext;
    }
    public synchronized void establishConnection(final BluetoothDevice device){
        if ((!connectionStatus.containsKey(device) || connectionStatus.get(device) == 0) && !connecting) {
            connecting = true;
            connectionStatus.put(device, 1);
            Log.d(TAG, "Connecting to " + device.getAddress());
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    device.connectGatt(sContext, false, mGattCallback, 2);
                }
            };
            mainHandler.post(myRunnable);
        }
    }
    public void disableConnection(BluetoothGatt gatt){
        gatt.close();
        Log.d(TAG, "Device " + gatt.getDevice().getAddress() + " disconnected.");
    }

    private void sendAndroidID(BluetoothGatt gatt) {
        BluetoothGattCharacteristic toSendValue = gatt.getService(mServiceUUID).getCharacteristic(mAndroidIDCharUUID);
        toSendValue.setValue(android_id.getBytes());
        Log.d(TAG, "Sending Android ID: " + new String(toSendValue.getValue()));
        gatt.writeCharacteristic(toSendValue);
    }

    private void sendHERAMatrix(BluetoothGatt gatt) {
        Connection curConnection = mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(gatt.getDevice()));
        curConnection.setMyHERAMatrix(mHera.getReachabilityMatrix());
        curConnection.flattenMyHeraMatrix();
        BluetoothGattCharacteristic toSendValue = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
        toSendValue.setValue(mConnectionSystem.getToSendFragment(gatt,0, ConnectionSystem.DATA_TYPE_MATRIX));
        gatt.writeCharacteristic(toSendValue);
    }
    private void getNeighborAndroidID(BluetoothGatt gatt) {
        gatt.readCharacteristic(gatt.getService(mServiceUUID).getCharacteristic(mAndroidIDCharUUID));
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        String TAG = "BluetoothGattCallBack";
        @Override
        public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            connecting = false;
            if(newState == BluetoothGatt.STATE_CONNECTED){
                connectionStatus.put(gatt.getDevice(), 2);
                gatt.discoverServices();
            }
            else if(newState == BluetoothGatt.STATE_DISCONNECTED){
                mBLEHandler.updateMessageSystemUI();
                connectionStatus.put(gatt.getDevice(), 0);
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG,"Service discovered");
            if (mConnectionSystem.isBean(gatt)) {
                gatt.readCharacteristic(gatt.getService(BeanScratchServiceUUID).getCharacteristic(BeanScratchFirstCharUUID));
            }
            else if (mConnectionSystem.isHERANode(gatt)) {
                getNeighborAndroidID(gatt);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d(TAG, "MTU changed with " + gatt.getDevice() + " to " + mtu);
                mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(gatt.getDevice())).setClientMTU(mtu);
                sendAndroidID(gatt);
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            String TAG = "CharacteristicRead";
            if (mConnectionSystem.isBean(gatt)) {
                int scratchNumber = Integer.valueOf(characteristic.getUuid().toString().substring(7, 8));
                System.out.println("Scratch " + scratchNumber + " read");
                System.out.println(ConnectionSystem.bytesToHex(characteristic.getValue()));
                if (scratchNumber < 5)
                    gatt.readCharacteristic(gatt.getService(BeanScratchServiceUUID).getCharacteristic(UUID.fromString("A495FF2" + String.valueOf(scratchNumber + 1) + "-C5B1-4B44-B512-1370F02D74DE")));
            }
            else if (mConnectionSystem.isHERANode(gatt)) {
                String neighborAndroidID = characteristic.getStringValue(0);
                Log.d(TAG, "neighbor Android ID: " + neighborAndroidID);
                if (mConnectionSystem.getConnection(neighborAndroidID) == null || mConnectionSystem.getConnection(neighborAndroidID).getLastConnectedTimeDiff() >= 20   ) {

                    mConnectionSystem.updateConnection(neighborAndroidID, gatt);
                    gatt.requestMtu(BLEHandler._mtu);
                    Log.d(TAG, "Requesting mtu change of " + BLEHandler._mtu);
                }
                else {
                    Log.d(TAG, "Saw " + neighborAndroidID + " recently");
                    gatt.disconnect();
                }
            }
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            String TAG = "CharacteristicWrite";
            int dataType = characteristic.getValue()[0];
            int prevSegCount = characteristic.getValue()[1];
            int isLast = characteristic.getValue()[2];

            if (characteristic.getUuid().equals(mAndroidIDCharUUID)) {
                Log.d(TAG, "Android ID sent, sending HERA Matrix");
                sendHERAMatrix(gatt);
                return;
            }
            Connection curConnection = mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(gatt.getDevice()));
            if(isLast == 0) {
                Log.d(TAG, "All " + (prevSegCount + 1) + " segments have been transmitted");
                if (dataType == ConnectionSystem.DATA_TYPE_MATRIX) {
                    if (!curConnection.isToSendQueueEmpty()) {
                        Log.d(TAG, "toSend is not empty, sending message");
                        mBLEHandler.sendMessage(curConnection);
                    }
                    else {
                        curConnection.updateLastConnectedTime();
                        gatt.disconnect();
                    }
                }
                if (dataType == ConnectionSystem.DATA_TYPE_MESSAGE) {
                    mMessageSystem.MessageAck(curConnection);
                    mBLEHandler.updateMessageSystemUI();
                    if (!curConnection.isToSendQueueEmpty()) {
                        Log.d(TAG, "Message delivered, sending next message");
                        mBLEHandler.sendMessage(curConnection);
                    }
                    else {
                        Log.d(TAG, "All messages have been transmitted, the connection will now terminate");
                        curConnection.updateLastConnectedTime();
                        gatt.disconnect();
                    }
                }
            }
            else {
                BluetoothGattCharacteristic segmentToSend = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
                segmentToSend.setValue(mConnectionSystem.getToSendFragment(gatt, prevSegCount + 1, ConnectionSystem.DATA_TYPE_MATRIX));
                gatt.writeCharacteristic(segmentToSend);
                Log.d(TAG, "Fragment " + (prevSegCount + 1) + " sent");
            }
        }
    };

}
