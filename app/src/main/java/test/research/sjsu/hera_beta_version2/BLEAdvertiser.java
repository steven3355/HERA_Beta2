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
        mAdvertiseSettingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
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
