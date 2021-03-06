package test.research.sjsu.hera_beta_version2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

import static test.research.sjsu.hera_beta_version2.BLEHandler.BeanScratchFirstCharUUID;
import static test.research.sjsu.hera_beta_version2.BLEHandler.BeanScratchServiceUUID;
import static test.research.sjsu.hera_beta_version2.BLEHandler.connecting;
import static test.research.sjsu.hera_beta_version2.BLEHandler.connectionStatus;
import static test.research.sjsu.hera_beta_version2.BLEHandler.mAndroidIDCharUUID;
import static test.research.sjsu.hera_beta_version2.BLEHandler.mCharUUID;
import static test.research.sjsu.hera_beta_version2.BLEHandler.mServiceUUID;
import static test.research.sjsu.hera_beta_version2.BLEHandler.transmitting;
import static test.research.sjsu.hera_beta_version2.MainActivity.android_id;
import static test.research.sjsu.hera_beta_version2.MainActivity.mBLEHandler;
import static test.research.sjsu.hera_beta_version2.MainActivity.mConnectionSystem;
import static test.research.sjsu.hera_beta_version2.MainActivity.mHera;
import static test.research.sjsu.hera_beta_version2.MainActivity.mMessageSystem;
import static test.research.sjsu.hera_beta_version2.MainActivity.mUiManager;


/**
 * BLEClient
 * Initializes connections between BLE devices and performs the following overheads for HERA Routing
 * 1. Connection is established.
 * 2. Performs a service discovery to determine the type of node of the other device (All of the HERA nodes would have the same serviceUUID, but the Bean+ end nodes would have another)
 * 3a. If the other device is a Bean+ start node, performs read request to get stored data in the scratch memory of the Bean+ devices.
 * 3b. If the other device is a Bean+ end node, performs write request to send the end node messages if there are messages in the MessageQueue.
 * 3c. If the other device is a HERA node (Android device), performs a read request on characteristicUUID 0x3001 on the connected device to get the other device's unique identifier
 *     (Read more device unique identifiers in the MainActivity Class file)
 * 4. Change the connection MTU (So we can reduce the number of fragments when transmitting larger frames)
 * 5. Send our device's unique identifier (So the neighbor knows who we are, because the connections are pretty unidirectional)
 * 6. Check the previous time the HERA reachability matrix was exchanged. (We don't want to update too often when nodes stay near each other)
 * 7. If time is shorter than the cooldown interval, skip to step 9.
 * 8. Send a copy of our HERA reachability matrix (So the neighbor can update their own reachability matrix, and then they make the decision of whether to send some packets to us using the HERA routing scheme)
 * 9. If we've already received the neighbor's reachability matrix through another asynchronous unidirectional connection
 *    (in which the other device is the client), and we have already computed that we have messages to send to this other device, start the message transmitting process via characteristicWrite(See more in BLEHandler Class file)
 * 10. We receive an acknowledgement for the characterWrite
 * 11a. If the fragment we sent was not the last fragment, send next fragment
 * 11b. If the fragment we sent was the last fragment, but there are more messages to send, send next message
 * 11c. If the fragment we sent was the last fragment, and there are no more message to send, terminates the connection
 *
 * Created by Steven on 3/13/2018.
 */

class BLEClient {

    private Context sContext;
    private String TAG = "BLEClient";
    BLEClient(Context systemContext) {
        sContext = systemContext;
    }

    /**
     * Connect to a device
     * We are running the process on the main thread to avoid potential problem
     * @param device
     */
    void establishConnection(final BluetoothDevice device){
        if ((!connectionStatus.containsKey(device) || connectionStatus.get(device) == 0) && !connecting) {
            connecting = true;
            connectionStatus.put(device, 1);
            Log.d(TAG, "Connecting to " + device.getAddress());
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    device.connectGatt(sContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
                }
            };
            mainHandler.post(myRunnable);
        }
    }
    void disableConnection(BluetoothGatt gatt){
        gatt.close();
        Log.d(TAG, "Device " + gatt.getDevice().getAddress() + " disconnected.");
    }

    /**
     * Send our Android ID
     * @param gatt
     */
    private void sendAndroidID(BluetoothGatt gatt) {
        BluetoothGattCharacteristic toSendValue = gatt.getService(mServiceUUID).getCharacteristic(mAndroidIDCharUUID);
        toSendValue.setValue(android_id.getBytes());
//        Log.d(TAG, "Sending Android ID: " + new String(toSendValue.getValue()));
        gatt.writeCharacteristic(toSendValue);
    }

    /**
     * read neighbor's Android ID
     * @param gatt
     */
    private void getNeighborAndroidID(BluetoothGatt gatt) {
        gatt.readCharacteristic(gatt.getService(mServiceUUID).getCharacteristic(mAndroidIDCharUUID));
    }

    /**
     * Stores a copy of the current HERA matrix
     * Flatten the matrix to byte array to send
     * Send the first fragment
     * @param gatt
     */
    private void sendHERAMatrix(BluetoothGatt gatt) {
        Connection curConnection = mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(gatt));
        curConnection.setMyHERAMatrix(mHera.getReachabilityMatrix());
        curConnection.flattenMyHeraMatrix();
        BluetoothGattCharacteristic toSendValue = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
        toSendValue.setValue(mConnectionSystem.getToSendFragment(gatt,0, ConnectionSystem.DATA_TYPE_MATRIX));
        gatt.writeCharacteristic(toSendValue);
    }

    /**
     * GattCallback
     * determines what to do when we receive ACKs for our actions
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        String TAG = "BluetoothGattCallBack";
        /**
         * When the connection state change to connect, start the service discovery process, update connection state map (See more in BLEHandler Class file)
         * When the connection disconnects, update connection state map
         */
        @Override
        public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED){
                connectionStatus.put(gatt.getDevice(), 2);
                connecting = false;
                if (mConnectionSystem.isBean(gatt)) {
                    mConnectionSystem.updateConnection(gatt.getDevice().getAddress(), gatt);
                    Connection curConnection = mConnectionSystem.getConnection(gatt.getDevice().getAddress());
                    if (mConnectionSystem.isStartBean(gatt)) {
                        if (curConnection.getLastConnectedTimeDiff() >= HERA.REACH_COOL_DOWN_PERIOD) {
                            gatt.readCharacteristic(gatt.getService(BeanScratchServiceUUID).getCharacteristic(BeanScratchFirstCharUUID));
                        }
                        else {
                            gatt.disconnect();
                        }
                    }
                    else if (mConnectionSystem.isEndBean(gatt)) {
                        if (mMessageSystem.hasMessage("987bf3583813")) {
                            mBLEHandler.sendMessageToEnd(gatt);
                        }
                        else {
                            gatt.disconnect();
                        }
                    }
                }
                else {
                    gatt.discoverServices();
                }
            }
            else if (newState == BluetoothGatt.STATE_CONNECTING) {

            }
            else if (newState == BluetoothGatt.STATE_DISCONNECTED){
                connecting = false;
                connectionStatus.put(gatt.getDevice(), 0);
            }
        }

        /**
         * onServiceDiscovered
         * When service discovered, determines if the device is Bean+ end node or Android HERA node
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG,"Service discovered");
            if (mConnectionSystem.isBean(gatt)) {
                mConnectionSystem.updateConnection(gatt.getDevice().getAddress(), gatt);
                Connection curConnection = mConnectionSystem.getConnection(gatt.getDevice().getAddress());
                if (mConnectionSystem.isStartBean(gatt)) {
                    if (curConnection.getLastConnectedTimeDiff() >= HERA.REACH_COOL_DOWN_PERIOD) {
                        gatt.readCharacteristic(gatt.getService(BeanScratchServiceUUID).getCharacteristic(BeanScratchFirstCharUUID));
                    }
                    else {
                        gatt.disconnect();
                    }
                }
                else if (mConnectionSystem.isEndBean(gatt)) {
                    if (curConnection.getLastConnectedTimeDiff() >= 20) {
                        mHera.updateDirectHop("987bf3583813");
                        curConnection.updateLastConnectedTime();
                        mUiManager.updateHERAMatrixUI();
                    }
                    if (mMessageSystem.hasMessage("987bf3583813")) {
                        mBLEHandler.sendMessageToEnd(gatt);
                    }
                    else {
                        gatt.disconnect();
                    }
                }
            }
            else if (mConnectionSystem.isHERANode(gatt)) {
                getNeighborAndroidID(gatt);
            }
        }

        /**
         * onMtuChanged
         * send our Android ID
         */
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d(TAG, "MTU changed with " + gatt.getDevice() + " to " + mtu);
                mConnectionSystem.getConnection(mConnectionSystem.getAndroidID(gatt)).setClientMTU(mtu);
                sendAndroidID(gatt);
            }
        }

        /**
         * onCharacteristicRead
         * If neighbor device is Bean+, read all of the scratch memory
         * If neighbor device is Android, continue with connection process, request an mtu change
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            String TAG = "CharacteristicRead";
            if (mConnectionSystem.isBean(gatt)) {
                Connection curConnection = mConnectionSystem.getConnection(gatt.getDevice().getAddress());
                int scratchNumber = Integer.valueOf(characteristic.getUuid().toString().substring(7, 8));
                System.out.println("Scratch " + scratchNumber + " read");
                System.out.println(ConnectionSystem.bytesToHex(characteristic.getValue()));
                if (scratchNumber == 1) {
                    curConnection.writeToCache("987bf3583813".getBytes());
                }
                if (scratchNumber < 3) {
                    curConnection.writeToCache(characteristic.getValue());
                    gatt.readCharacteristic(gatt.getService(BeanScratchServiceUUID).getCharacteristic(UUID.fromString("A495FF2" + String.valueOf(scratchNumber + 1) + "-C5B1-4B44-B512-1370F02D74DE")));
                }
                else if (scratchNumber == 3) {
                    curConnection.buildMessage(12);
                    curConnection.resetCache();
                    curConnection.updateLastConnectedTime();
                    gatt.disconnect();
                }
            }
            else if (mConnectionSystem.isHERANode(gatt)) {
                String neighborAndroidID = characteristic.getStringValue(0);
                Log.d(TAG, "neighbor Android ID: " + neighborAndroidID);

                mConnectionSystem.updateConnection(neighborAndroidID, gatt);
                gatt.requestMtu(BLEHandler._mtu);
                Log.d(TAG, "Requesting mtu change of " + BLEHandler._mtu);
            }
        }

        /**
         * onCharacteristicWrite
         * Determines what to send based on what the ACK says we sent
         * If we sent Android ID, send HERA Matrix
         * If we sent HERA Matrix, see if there is message to send
         * If we sent message, check if last fragment
         * If we sent last message, connection finished
         */

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            String TAG = "CharacteristicWrite";
            /**
             * When the neighbor is a Bean+ end node and we've already sent the first part of the message by write request.
             */
            if (mConnectionSystem.isBean(gatt)) {
                int scratchNumber = Integer.valueOf(characteristic.getUuid().toString().substring(7, 8));
                if (scratchNumber < 3) {
                    BluetoothGattCharacteristic toSend = gatt.getService(BeanScratchServiceUUID).getCharacteristic(UUID.fromString("A495FF2" + String.valueOf(scratchNumber + 1) + "-C5B1-4B44-B512-1370F02D74DE"));
                    switch(scratchNumber) {
                        case 1:
                            toSend.setValue(Arrays.copyOfRange(mMessageSystem.getMessage("987bf3583813").getData(), 1, 18));
                            break;
                        case 2:
                            toSend.setValue(Arrays.copyOfRange(mMessageSystem.getMessage("987bf3583813").getData(), 18, 20));
                            break;
                    }
                    gatt.writeCharacteristic(toSend);
                    Log.d(TAG, new String(characteristic.getValue()));
                }
                else if (scratchNumber == 3) {
                    mMessageSystem.getMessageQueue("987bf3583813").remove();
                    mUiManager.updateMessageSystemUI();
                    if(mMessageSystem.hasMessage("987bf3583813")) {
                        try {
                            Thread.sleep(1000);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        mBLEHandler.sendMessageToEnd(gatt);
                    }
                    else {
                        gatt.disconnect();
                    }
                }
                return;
            }


            int dataType = characteristic.getValue()[0];
            int prevSegCount = characteristic.getValue()[1];
            int isLast = characteristic.getValue()[2];
            String neighborAndroidID = mConnectionSystem.getAndroidID(gatt);
            Connection curConnection = mConnectionSystem.getConnection(neighborAndroidID);
            /**if we completed the first step of our handshake process, which is send device android ID,
            * then depending on when the last HERA matrix was formed with the neighboring device,
            * we'll decide if we want to send HERA matrix again or just check our to send queue.
            */
            if (characteristic.getUuid().equals(mAndroidIDCharUUID)) {
                if (mConnectionSystem.getConnection(neighborAndroidID).getLastConnectedTimeDiff() >= HERA.REACH_COOL_DOWN_PERIOD) {
                    curConnection.updateLastConnectedTime();
                    sendHERAMatrix(gatt);
                }
                else if (!curConnection.isToSendQueueEmpty()){
                    mBLEHandler.sendMessage(curConnection);
                }
                return;
            }
            /*if we finished sending the current packet, we then decide what to do next, if we have something more to send, keep sending, if not, terminate the connection*/
            if(characteristic.getUuid().equals(mCharUUID) && isLast == 0) {
                Log.d(TAG, "All " + (prevSegCount + 1) + " segments have been transmitted");
                if (dataType == ConnectionSystem.DATA_TYPE_MATRIX) {
                    if (!curConnection.isToSendQueueEmpty()) {
                        Log.d(TAG, "toSend is not empty, sending message");
                        mBLEHandler.sendMessage(curConnection);
                    }
                    else {
                        transmitting = false;
                        gatt.disconnect();
                    }
                }
                if (dataType == ConnectionSystem.DATA_TYPE_MESSAGE) {
                    mMessageSystem.MessageAck(curConnection);
                    mUiManager.updateMessageSystemUI();
                    if (!curConnection.isToSendQueueEmpty()) {
                        Log.d(TAG, "Message delivered, sending next message");
                        mBLEHandler.sendMessage(curConnection);
                    }
                    else {
                        Log.d(TAG, "All messages have been transmitted, the connection will now terminate");
                        transmitting = false;
                        gatt.disconnect();
                    }
                }
            }
            /*Not the last fragment to the current packet, send next fragment*/
            else if (characteristic.getUuid().equals(mCharUUID) && isLast != 0){
                BluetoothGattCharacteristic segmentToSend = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
                segmentToSend.setValue(mConnectionSystem.getToSendFragment(gatt, prevSegCount + 1, ConnectionSystem.DATA_TYPE_MATRIX));
                gatt.writeCharacteristic(segmentToSend);
                Log.d(TAG, "Fragment " + (prevSegCount + 1) + " sent");
            }
        }
    };

}
