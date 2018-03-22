package test.research.sjsu.hera_beta_version2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static test.research.sjsu.hera_beta_version2.MainActivity.android_id;
import static test.research.sjsu.hera_beta_version2.MainActivity.mUiManager;

/**
 * HERA
 * HERA Routing Scheme
 * See more at
 * https://www.dropbox.com/s/vobgpj6lbmsft68/paper.pdf?dl=0
 * Created by Steven on 3/13/2018.
 */

public class HERA implements RoutingAlgorithm {

    static private final int DEFAULT_HOPS_SIZE = 5;
    static private final double[] DEFAULT_LAMBDA = {1, .5, .05, .005, .0005};
    static private final double[] DEFAULT_GAMMA = {1, .5, .05, .005, .0005};
    static private final double DEFAULT_ALPHA = 0.98;
    static private final int DEFAULT_AGING_PERIOD = 10;
    static final int REACH_COOL_DOWN_PERIOD = 20;

    private int H = 5;
    private double[] lambda = {1, 0.5, 0.05, 0.005, 0.0005},
            gamma = {1, 0.5, 0.05, 0.005, 0.0005};
    private double alpha = 0.98;
    private HERAMatrix mHERAMatrix;
    private static ScheduledExecutorService matrixAger = Executors.newSingleThreadScheduledExecutor();
    private int ageTime;
    private String TAG = "HERA";
    HERA() {
        H = DEFAULT_HOPS_SIZE;
        lambda = DEFAULT_LAMBDA;
        gamma = DEFAULT_GAMMA;
        alpha = DEFAULT_ALPHA;
        ageTime = DEFAULT_AGING_PERIOD;
        mHERAMatrix = new HERAMatrix();
        matrixAger.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        ageMatrix();
//                        Log.d(TAG, "Hera matrix aged");
                    }
                }
                , 1, ageTime, TimeUnit.SECONDS
        );
    }
//    public HERA(Map<String, List<Double>> neighborMatrix) {
//        reachabilityMatrix = neighborMatrix;
//    }
    public HERA(int maxHop, double agingConstant, double[] intrinsicConfidence, double[] weight, int agingTime, String selfAddress) {
        H = maxHop;
        alpha = agingConstant;
        lambda = intrinsicConfidence;
        gamma = weight;
        ageTime = agingTime;
        mHERAMatrix = new HERAMatrix();
        matrixAger.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        ageMatrix();
//                        Log.d(TAG, "Hera matrix aged");
                    }
                }
                , 0, ageTime, TimeUnit.SECONDS
        );
    }
    HERAMatrix getReachabilityMatrix() {
        return mHERAMatrix;
    }
//    public List<Double> getHopsReachability(String dest) {
//        if (!reachabilityMatrix.containsKey(dest)) {
//            reachabilityMatrix.put(dest, new ArrayList<>(Collections.nCopies(H, 0.0)));
//        }
//        return reachabilityMatrix.get(dest);
//    }
//    public double getHopReachability(String dest, int hop) {
//        if (!reachabilityMatrix.containsKey(dest)) {
//            reachabilityMatrix.put(dest, new ArrayList<>(Collections.nCopies(H, 0.0)));
//        }
//        return reachabilityMatrix.get(dest).get(hop);
//    }
    private double getReachability(String dest, HERAMatrix heraMatrix) {
        double weightedSum = 0;
        if (!heraMatrix.containsKey(dest)) {
//            reachabilityMatrix.put(dest, new ArrayList<>(Collections.nCopies(H, 0.0)));
            return 0;
        }
        List<Double> hopReachabilities = heraMatrix.get(dest);
        for (int i = 0; i < H; i++) {
            weightedSum += gamma[i] * hopReachabilities.get(i);
        }
        return weightedSum;
    }

    void updateDirectHop(String neighbor) {
        if (!mHERAMatrix.containsKey(neighbor)) {
            mHERAMatrix.put(neighbor, new ArrayList<>(Collections.nCopies(H, 0.0)));
            mHERAMatrix.get(neighbor).set(0, lambda[0]);
        }
        else {
            mHERAMatrix.get(neighbor).set(0 , mHERAMatrix.get(neighbor).get(0) + 1);
        }
        mUiManager.updateHERAMatrixUI();
    }
    void updateTransitiveHops(HERAMatrix neighborMap) {
        for(Map.Entry<String, List<Double>> entry : neighborMap.entrySet()) {
            if (entry.getKey().equals(android_id)) {
                continue;
            }
            if (!mHERAMatrix.containsKey(entry.getKey())) {
                mHERAMatrix.put(entry.getKey(), new ArrayList<>(Collections.nCopies(H, 0.0)));
            }
            List<Double> updateHopList = mHERAMatrix.get(entry.getKey());
            for (int h = 1; h < H; h++) {
                double delta = lambda[h] * entry.getValue().get(h - 1);
                updateHopList.set(h, updateHopList.get(h) + delta);
            }
            mHERAMatrix.put(entry.getKey(), updateHopList);
        }
        mUiManager.updateHERAMatrixUI();
    }
    private void ageMatrix() {
        for (Map.Entry<String, List<Double>> entry :mHERAMatrix.entrySet()) {
            List<Double> curList = entry.getValue();
            for (int i = 0; i < H; i++) {
                curList.set(i, curList.get(i) * alpha);
            }
            mHERAMatrix.put(entry.getKey(), curList);
        }
        mUiManager.updateHERAMatrixUI();
    }

    boolean makeDecision(String dest, HERAMatrix neighborHERAMatrix) {
        double myReachability = getReachability(dest, mHERAMatrix);
        double neighborReachability = getReachability(dest, neighborHERAMatrix);
        return neighborReachability > myReachability;
    }
}