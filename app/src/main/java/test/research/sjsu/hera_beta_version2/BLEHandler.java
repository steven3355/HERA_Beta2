package test.research.sjsu.hera_beta_version2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static test.research.sjsu.hera_beta_version2.MainActivity.mConnectionSystem;
import static test.research.sjsu.hera_beta_version2.MainActivity.mMessageSystem;
import static test.research.sjsu.hera_beta_version2.MainActivity.mUiManager;

/**
 * BLE Handler
 * Handles the instance(s) of BLEAdvertiser, BLEScanner, BLEServer, and BLEClient
 *
 * Created by Steven on 3/13/2018.
 */

class BLEHandler {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BLEAdvertiser mBLEAdvertiser;
    private BLEScanner mBLEScanner;
    private BLEServer mBLEServer;
    private BLEClient mBLEClient;

    static Map<BluetoothDevice, Integer> connectionStatus;

    static final ParcelUuid mServiceUUIDParcel = ParcelUuid.fromString("00001830-0000-1000-8000-00805F9B34FB");
    static final ParcelUuid BeanServiceUUID = ParcelUuid.fromString("A495FF10-C5B1-4B44-B512-1370F02D74DE");
    static final ParcelUuid BeanServiceMask = ParcelUuid.fromString("11111100-0000-0000-0000-000000000000");
    static final UUID mServiceUUID = UUID.fromString("00001830-0000-1000-8000-00805F9B34FB");
    static final UUID mCharUUID = UUID.fromString("00003000-0000-1000-8000-00805f9b34fb");
    static final UUID mAndroidIDCharUUID = UUID.fromString("00003001-0000-1000-8000-00805f9b34fb");
    static final UUID BeanScratchServiceUUID = UUID.fromString("A495FF20-C5B1-4B44-B512-1370F02D74DE");
    static final UUID BeanScratchFirstCharUUID = UUID.fromString("A495FF21-C5B1-4B44-B512-1370F02D74DE");

    static final int _mtu = 400;
    Context sContext;
    static boolean connecting = false;
    static boolean transmitting = false;

    private String TAG = "BLEHandler";
    BLEHandler(Context systemContext) {
        sContext = systemContext;
        connectionStatus = new HashMap<>();
        mBluetoothManager = (BluetoothManager) sContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBLEClient = new BLEClient(sContext);
    }

    /**
     * build the message into byte array form for the current connection
     * @param curConnection
     */
    private void prepareToSendMessage(Connection curConnection) {
        String dest = curConnection.getOneToSendDestination();
        Log.d(TAG, "Preparing to send message for: " + dest);
        curConnection.setCurrentToSendPacket(mMessageSystem.getMessage(dest).getByte());
    }

    /**
     * if there is an instance of a gatt client for the current connection, builds and sends the first segment of the packet
     * @param curConnection
     */
    void sendMessage(Connection curConnection) {
        String TAG = "sendMessage";
        BluetoothGatt gatt = curConnection.getGatt();
        if (gatt != null) {
            Log.d(TAG, "client gatt found, sending message");
            prepareToSendMessage(curConnection);
            BluetoothGattCharacteristic toSend = gatt.getService(mServiceUUID).getCharacteristic(mCharUUID);
            toSend.setValue(mConnectionSystem.getToSendFragment(gatt, 0, ConnectionSystem.DATA_TYPE_MESSAGE));
            gatt.writeCharacteristic(toSend);
        }
        else {
            transmitting = false;
        }
    }


    /**
     * establish a connection given a temporary BLE address
     * @param address
     */
    void establishConnection (String address) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mBLEClient.establishConnection(device);
    }

    /**
     * Instantiate an BLEServer class
     */
    private void initializeBLEGattServer() {
        mBLEServer = new BLEServer(mBluetoothManager, sContext);
        mBLEServer.startServer();
    }

    /**
     * Instantiate an BLEAdvertiser class
     */
    private void initializeBLEAdvertiser() {
        mBLEAdvertiser = new BLEAdvertiser(mBluetoothAdapter.getBluetoothLeAdvertiser());
        mBLEAdvertiser.prepareAdvertiseData();
        mBLEAdvertiser.prepareAdvertiseSettings();
        mBLEAdvertiser.startAdvertise();
        mUiManager.updateMessageSystemUI();
    }

    /**
     * Instantiate an BLEScanner class
     */
    private void initializeBLEScanner() {
        mBLEScanner = new BLEScanner(mBluetoothAdapter.getBluetoothLeScanner());
        mBLEScanner.prepareScanFilter();
        mBLEScanner.prepareScanSetting();
        mBLEScanner.startScan();
    }

    /**
     * Check if Bluetooth Adapter is on before initializing BLE Advertiser, Scanner and Servers.
     * if Bluetooth Adapter isn't on, turn it on
     */
    void initilizeBLEServices() {
        if(!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            ((Activity)sContext).runOnUiThread(new Runnable() {
                public void run() {
                    TextView systemStatus = ((Activity)sContext).findViewById(R.id.HERAMatrixUI);
                    systemStatus.setText("Turning on Bluetooth Adapter");
                }
            });
            try {
                Thread.sleep(1000);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        initializeBLEGattServer();
        initializeBLEAdvertiser();
        initializeBLEScanner();
    }

    /**
     * Stops the BLE GATT Server service
     */
    private void terminateBLEGattServer() {
        mBLEServer.stopServer();
    }

    /**
     * Stops BLE advertisement
     */
    private void terminateBLEAdvertiser() {
        mBLEAdvertiser.stopAdvertise();
    }

    /**
     * Stops BLE beacon scan and discovery
     */
    private void terminateBLEScanner() {
        mBLEScanner.stopScan();
        mBluetoothAdapter.cancelDiscovery();
    }

    /**
     * Close gatt connection instance for all connected device
     */
    private void terminateBLEClients() {
        List<Connection> connectionList = mConnectionSystem.getConnectionList();
        for (Connection connection : connectionList) {
            if (connection.getGatt() != null) {
                connection.getGatt().close();
            }
        }
    }

    /**
     * Terminates all BLE services
     */
    void terminateBLEServices() {
        terminateBLEGattServer();
        terminateBLEAdvertiser();
        terminateBLEScanner();
        terminateBLEClients();
    }
}
