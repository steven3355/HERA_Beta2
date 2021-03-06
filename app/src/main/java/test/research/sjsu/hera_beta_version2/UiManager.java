package test.research.sjsu.hera_beta_version2;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import static test.research.sjsu.hera_beta_version2.MainActivity.mHera;
import static test.research.sjsu.hera_beta_version2.MainActivity.mMessageSystem;

/**
 * UiManager
 * Handles all Ui updates
 * Created by Steven on 3/19/2018.
 */

class UiManager {
    private Context sContext;
    UiManager(Context systemContext) {
        sContext = systemContext;
    }

    /**
     * updates the current state of the HERA Matrix on the UI
     * seperates each entry by line
     * limit each number to two decimal points
     */
    void updateHERAMatrixUI() {
        ((Activity)sContext).runOnUiThread(new Runnable() {
            public void run() {
                StringBuilder matrixString = new StringBuilder();
                for (Map.Entry<String, List<Double>> entry : mHera.getReachabilityMatrix().entrySet()) {
                    matrixString.append(entry.getKey() + ": [");
                    for (Double num : entry.getValue()) {
                        matrixString.append(Math.round(num * 100.0) / 100.0 + ", ");
                    }
                    matrixString.replace(matrixString.length() - 2, matrixString.length(), "]\n");
                }
                TextView HERAStatus =((Activity)sContext).findViewById(R.id.HERAMatrixUI);
                HERAStatus.setText(matrixString.toString());
            }
        });
    }

    /**
     * updates the current state of the Message System on the UI
     */

    void updateMessageSystemUI() {
        StringBuilder statusBuilder = new StringBuilder();
        for(Map.Entry<String,Queue<Message>> entry : mMessageSystem.getMessageMap().entrySet()) {
            statusBuilder.append(entry.getKey());
            statusBuilder.append(": ");
            statusBuilder.append(entry.getValue().size());
            statusBuilder.append("\n");
        }
        final String status = statusBuilder.toString();
        ((Activity)sContext).runOnUiThread(new Runnable() {
            public void run() {
                TextView messageStatus = ((Activity)sContext).findViewById(R.id.MessageSystemStatusUI);
                messageStatus.setText(status);
            }
        });
    }
}
