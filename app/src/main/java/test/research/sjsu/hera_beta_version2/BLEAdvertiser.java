package test.research.sjsu.hera_beta_version2;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.util.Log;

/**
 * BLEAdvertiser
 * Sends BLE Beacon class
 * Created by Steven on 3/13/2018.
 */

class BLEAdvertiser {
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseData mAdvertiseData;
    private AdvertiseSettings mAdvertiseSettings;
    private String TAG = "BLEAdvertiser";

    BLEAdvertiser(BluetoothLeAdvertiser BLEAdvertiser) {
        mBluetoothLeAdvertiser = BLEAdvertiser;
    }

    void prepareAdvertiseData() {
        AdvertiseData.Builder mAdvertiseDataBuilder = new AdvertiseData.Builder();
        mAdvertiseDataBuilder.addServiceUuid(BLEHandler.mServiceUUIDParcel);
        mAdvertiseDataBuilder.setIncludeTxPowerLevel(true);
        mAdvertiseDataBuilder.setIncludeDeviceName(true);
        mAdvertiseData = mAdvertiseDataBuilder.build();
    }

    void prepareAdvertiseSettings() {
        AdvertiseSettings.Builder mAdvertiseSettingsBuilder = new AdvertiseSettings.Builder();
        mAdvertiseSettingsBuilder.setConnectable(true);
        /*
        There are 3 Advertise mode:
        int ADVERTISE_MODE_LOW_POWER   = 0x00 (1Hz)
        int ADVERTISE_MODE_BALANCED    = 0x01 (3Hz)
        int ADVERTISE_MODE_LOW_LATENCY = 0x02 (10Hz)
        Timeout may not exceed 180000 milliseconds. A value of 0 will disable the time limit.
         */
        mAdvertiseSettingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        /*
        There are 4 Transfer Power Level
        int ADVERTISE_TX_POWER_ULTRA_LOW = 0x00
        int ADVERTISE_TX_POWER_LOW       = 0x01
        int ADVERTISE_TX_POWER_MEDIUM    = 0x02
        int ADVERTISE_TX_POWER_HIGH      = 0x03
         */
        mAdvertiseSettingsBuilder.setTimeout(0);
        mAdvertiseSettingsBuilder.setTxPowerLevel(3);
        mAdvertiseSettings = mAdvertiseSettingsBuilder.build();
    }

    void startAdvertise() {
        if (mAdvertiseData == null || mAdvertiseSettings == null) {
            Log.d(TAG, "startAdvertise aborted");
            return;
        }
        mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mAdvertiseCallback);
        Log.d(TAG, "Advertisement initiated");
    }

    void stopAdvertise() {
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        Log.d(TAG, "Advertisement terminated");
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

        }
    };
}
