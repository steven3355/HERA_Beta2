package test.research.sjsu.hera_beta_version2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    static BLEHandler mBLEHandler;
    static HERA mHera;
    static ConnectionSystem mConnectionSystem;
    static MessageSystem mMessageSystem;
    private String TAG = "MainActivity";
    static String android_id;
    private final int PERMISSION_REQUEST_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        mHera = new HERA();
        mConnectionSystem = new ConnectionSystem();
        mMessageSystem = new MessageSystem();
        TextView name = (TextView) findViewById(R.id.androidIDUI);
        name.setText("My android ID: " + android_id);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission check failed");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
        else {
            initilizeApplication();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initilizeApplication();
            }
            else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLEHandler.terminateBLEServices();
    }

    private void initilizeApplication() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission check failed");
            return;
        }
        Log.d(TAG, "Android ID: " + android_id);
        Log.d(TAG, "Permission check passed");

        mBLEHandler = new BLEHandler(this);
        mBLEHandler.initilizeBLEServices();
    }


}
