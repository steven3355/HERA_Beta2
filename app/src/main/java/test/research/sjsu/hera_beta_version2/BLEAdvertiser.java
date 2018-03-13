package test.research.sjsu.hera_beta_version2;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.util.Log;

/**
 * Created by Steven on 3/13/2018.
 */

public class BLEAdvertiser {
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseData mAdvertiseData;
    private AdvertiseSettings mAdvertiseSettings;
    private String TAG = "BLEAdvertiser";

    BLEAdvertiser(BluetoothLeAdvertiser BLEAdvertiser) {
        mBluetoothLeAdvertiser = BLEAdvertiser;
    }

    public void prepareAdvertiseData() {
        AdvertiseData.Builder mAdvertiseDataBuilder = new AdvertiseData.Builder();
        mAdvertiseDataBuilder.addServiceUuid(BLEHandler.mServiceUUIDParcel);
        mAdvertiseDataBuilder.setIncludeTxPowerLevel(true);
        mAdvertiseDataBuilder.setIncludeDeviceName(true);
        mAdvertiseData = mAdvertiseDataBuilder.build();
    }

    public void prepareAdvertiseSettings() {
        AdvertiseSettings.Builder mAdvertiseSettingsBuilder = new AdvertiseSettings.Builder();
        mAdvertiseSettingsBuilder.setConnectable(true);
        mAdvertiseSettingsBuilder.setTimeout(0);
        mAdvertiseSettingsBuilder.setTxPowerLevel(3);
        mAdvertiseSettings = mAdvertiseSettingsBuilder.build();
    }

    public void startAdvertise() {
        if (mAdvertiseData == null || mAdvertiseSettings == null) {
            Log.d(TAG, "startAdvertise aborted");
            return;
        }
        mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mAdvertiseCallback);
        Log.d(TAG, "Advertisement initiated");
    }

    public void stopAdvertise() {
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
