package test.research.sjsu.hera_beta_version2;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static test.research.sjsu.hera_beta_version2.MainActivity.mBLEHandler;

/**
 * BLEScanner
 * Scans for BLE Beacons
 * Created by Steven on 3/13/2018.
 */

public class BLEScanner {
    private BluetoothLeScanner mBluetoothLeScanner;
    private List<ScanFilter> mScanFilterList;
    private ScanSettings mScanSettings;
    private String TAG = "BLEScanner";

    BLEScanner(BluetoothLeScanner BLEScanner) {
        mBluetoothLeScanner = BLEScanner;
    }

    public void prepareScanFilter() {
        ScanFilter.Builder mScanFilterBuilder = new ScanFilter.Builder();
        List<ScanFilter> filterList = new ArrayList<>();
        mScanFilterBuilder.setServiceUuid(BLEHandler.mServiceUUIDParcel);
        ScanFilter.Builder mScanFilterBuilder2 = new ScanFilter.Builder();
        mScanFilterBuilder2.setServiceUuid(BLEHandler.BeanServiceUUID, BLEHandler.BeanServiceMask);
        filterList.add(mScanFilterBuilder2.build());
        filterList.add(mScanFilterBuilder.build());
        Log.d(TAG, "Scan filter prepared");
        mScanFilterList = filterList;
    }

    public void prepareScanSetting(){
        ScanSettings.Builder mScanSettingsBuilder = new ScanSettings.Builder();
        /*
        int SCAN_MODE_OPPORTUNISTIC   = -1
        (No active scan, only waits for advertisement)
        int SCAN_MODE_LOW_POWER       = 0x00
        (scan period 2.5 minutes, active scan for 1.5 second, a 1% duty cycle)
        int SCAN_MODE_BALANCED        = 0x01
        (scan period 15 seconds, active scan for 1.5 second, a 10% duty cycle)
        int SCAN_MODE_LOW_LATENCY     = 0x02
        (scan period 1.67 second, active scan for 1.5 second, a 90% duty cycle)
         */
        mScanSettingsBuilder.setScanMode(2);
        Log.d(TAG, "Scan Setting prepared");
        mScanSettings = mScanSettingsBuilder.build();
    }

    public void startScan() {
        if (mBluetoothLeScanner == null) {
            Log.d(TAG, "Cannot get BLEScanner, scan aborted");
            return;
        }
        if (mScanFilterList == null) {
            Log.d(TAG, "Cannot get scan filter, scan aborted");
            return;
        }
        if (mScanSettings == null) {
            Log.d(TAG, "Cannot get scan settings, scan aborted");
            return;
        }
        mBluetoothLeScanner.startScan(mScanFilterList, mScanSettings, mScanCallback);
        Log.d(TAG, "Scan initiated");
    }
    private ScanCallback mScanCallback = new ScanCallback() {
        String TAG = "ScanCallBack";
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final String address = result.getDevice().getAddress();
//            Connection curConnection = mConnectionSystem.getConnection(address);
//            if (curConnection == null || curConnection.getLastConnectedTimeDiff() >= HERA.REACH_COOL_DOWN_PERIOD)
            mBLEHandler.establishConnection(address);
        }
    };
    public void stopScan() {
        mBluetoothLeScanner.stopScan(mScanCallback);
        Log.d(TAG, "Scan terminated");
    }
}
