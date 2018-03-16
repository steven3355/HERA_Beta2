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
import static test.research.sjsu.hera_beta_version2.MainActivity.mHera;
import static test.research.sjsu.hera_beta_version2.MainActivity.mMessageSystem;

/**
 * Created by Steven on 3/13/2018.
 */

public class BLEHandler {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BLEAdvertiser mBLEAdvertiser;
    private BLEScanner mBLEScanner;
    private BLEServer mBLEServer;
    private BLEClient mBLEClient;

    public static Map<BluetoothDevice, Integer> connectionStatus;

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
    public static boolean connecting = false;
    public static boolean transmitting = false;

    private String TAG = "BLEHandler";
    BLEHandler(Context systemContext) {
        sContext = systemContext;
        connectionStatus = new HashMap<>();
        mBluetoothManager = (BluetoothManager) sContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBLEClient = new BLEClient(sContext);
    }

    private void prepareToSendMessage(Connection curConnection) {
        String dest = curConnection.getOneToSendDestination();
        Log.d(TAG, "Preparing to send message for: " + dest);
        curConnection.setCurrentToSendPacket(mMessageSystem.getMessage(dest).getByte());
    }


    public void sendMessage(Connection curConnection) {
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
    public void updateMessageSystemUI() {
        ((Activity)sContext).runOnUiThread(new Runnable() {
            public void run() {
                TextView messageStatus = (TextView)((Activity)sContext).findViewById(R.id.MessageSystemStatusUI);
                messageStatus.setText(mMessageSystem.updateMessageSystemStatusUI());
            }
        });
    }
    public void updateHERAMatrixUI() {
        ((Activity)sContext).runOnUiThread(new Runnable() {
            public void run() {
                TextView messageStatus = (TextView)((Activity)sContext).findViewById(R.id.HERAMatrixUI);
                messageStatus.setText(mHera.getReachabilityMatrix().toString());
            }
        });
    }

    public void establishConnection (String address) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mBLEClient.establishConnection(device);
    }

    private void initializeBLEGattServer() {
        mBLEServer = new BLEServer(mBluetoothManager, sContext);
        mBLEServer.startServer();
    }

    private void initializeBLEAdvertiser() {
        mBLEAdvertiser = new BLEAdvertiser(mBluetoothAdapter.getBluetoothLeAdvertiser());
        mBLEAdvertiser.prepareAdvertiseData();
        mBLEAdvertiser.prepareAdvertiseSettings();
        mBLEAdvertiser.startAdvertise();
        updateMessageSystemUI();
    }

    private void initializeBLEScanner() {
        mBLEScanner = new BLEScanner(mBluetoothAdapter.getBluetoothLeScanner());
        mBLEScanner.prepareScanFilter();
        mBLEScanner.prepareScanSetting();
        mBLEScanner.startScan();
    }

    public void initilizeBLEServices() {
        if(!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
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

    private void terminateBLEGattServer() {
        mBLEServer.stopServer();
    }

    private void terminateBLEAdvertiser() {
        mBLEAdvertiser.stopAdvertise();
    }

    private void terminateBLEScanner() {
        mBLEScanner.stopScan();
        mBluetoothAdapter.cancelDiscovery();
    }

    private void terminateBLEClients() {
        List<Connection> connectionList = mConnectionSystem.getConnectionList();
        for (Connection connection : connectionList) {
            if (connection.getGatt() != null) {
                connection.getGatt().close();
            }
        }
    }

    public void terminateBLEServices() {
        terminateBLEGattServer();
        terminateBLEAdvertiser();
        terminateBLEScanner();
        terminateBLEClients();
    }
}
