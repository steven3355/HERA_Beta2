package test.research.sjsu.hera_beta_version2;

import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    static BLEHandler mBLEHandler;
    static HERA mHera;
    static ConnectionSystem mConnectionSystem;
    static MessageSystem mMessageSystem;
    String TAG = "MainActivity";
    static String android_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        mHera = new HERA();
        mConnectionSystem = new ConnectionSystem();
        mMessageSystem = new MessageSystem();
        mBLEHandler = new BLEHandler(this);
        mBLEHandler.initilizeBLEServices();

        Log.d(TAG, "Android ID: " + android_id);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLEHandler.terminateBLEServices();

    }
}
