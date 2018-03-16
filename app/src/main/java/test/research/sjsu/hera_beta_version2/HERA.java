package test.research.sjsu.hera_beta_version2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static test.research.sjsu.hera_beta_version2.MainActivity.android_id;
import static test.research.sjsu.hera_beta_version2.MainActivity.mBLEHandler;

/**
 * Created by Steven on 3/13/2018.
 */

public class HERA implements Serializable {

    static final int DEFAULT_HOPS_SIZE = 5;
    static final double[] DEFAULT_LAMBDA = {1, .5, .05, .005, .0005};
    static final double[] DEFAULT_GAMMA = {1, .5, .05, .005, .0005};
    static final double DEFAULT_ALPHA = 0.98;
    static final int DEFAULT_AGING_PERIOD = 10;

    private int H = 5;
    private double[] lambda = {1, 0.5, 0.05, 0.005, 0.0005},
            gamma = {1, 0.5, 0.05, 0.005, 0.0005};
    private double alpha = 0.98;
    private Map<String, List<Double>> reachabilityMatrix;
    private static ScheduledExecutorService matrixAger = Executors.newSingleThreadScheduledExecutor();
    private int ageTime;
    private String TAG = "HERA";
    public HERA() {
        H = DEFAULT_HOPS_SIZE;
        lambda = DEFAULT_LAMBDA;
        gamma = DEFAULT_GAMMA;
        alpha = DEFAULT_ALPHA;
        ageTime = DEFAULT_AGING_PERIOD;
        reachabilityMatrix = new HashMap<>();
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
    public HERA(Map<String, List<Double>> neighborMatrix) {
        reachabilityMatrix = neighborMatrix;
    }
    public HERA(int maxHop, double agingConstant, double[] intrinsicConfidence, double[] weight, int agingTime, String selfAddress) {
        H = maxHop;
        alpha = agingConstant;
        lambda = intrinsicConfidence;
        gamma = weight;
        ageTime = agingTime;
        reachabilityMatrix = new HashMap<>();
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
    public Map<String, List<Double>> getReachabilityMatrix() {
        return reachabilityMatrix;
    }
    public List<Double> getHopsReachability(String dest) {
        if (!reachabilityMatrix.containsKey(dest)) {
            reachabilityMatrix.put(dest, new ArrayList<>(Collections.nCopies(H, 0.0)));
        }
        return reachabilityMatrix.get(dest);
    }
    public double getHopReachability(String dest, int hop) {
        if (!reachabilityMatrix.containsKey(dest)) {
            reachabilityMatrix.put(dest, new ArrayList<>(Collections.nCopies(H, 0.0)));
        }
        return reachabilityMatrix.get(dest).get(hop);
    }
    public double getReachability(String dest) {
        double weightedSum = 0;
        if (!reachabilityMatrix.containsKey(dest)) {
//            reachabilityMatrix.put(dest, new ArrayList<>(Collections.nCopies(H, 0.0)));
            return 0;
        }
        List<Double> hopReachabilities = reachabilityMatrix.get(dest);
        for (int i = 0; i < H; i++) {
            weightedSum += gamma[i] * hopReachabilities.get(i);
        }
        return weightedSum;
    }

    public void updateDirectHop(String neighbor) {
        if (!reachabilityMatrix.containsKey(neighbor)) {
            reachabilityMatrix.put(neighbor, new ArrayList<>(Collections.nCopies(H, 0.0)));
            reachabilityMatrix.get(neighbor).set(0, lambda[0]);
        }
        else {
            reachabilityMatrix.get(neighbor).set(0 , reachabilityMatrix.get(neighbor).get(0) + 1);
        }
        mBLEHandler.updateHERAMatrixUI();
    }
    public void updateTransitiveHops(String neighbor, Map<String, List<Double>> neighborMap) {
        for(Map.Entry<String, List<Double>> entry : neighborMap.entrySet()) {
            if (entry.getKey().equals(android_id)) {
                continue;
            }
            if (!reachabilityMatrix.containsKey(entry.getKey())) {
                reachabilityMatrix.put(entry.getKey(), new ArrayList<>(Collections.nCopies(H, 0.0)));
            }
            List<Double> updateHopList = reachabilityMatrix.get(entry.getKey());
            for (int h = 1; h < H; h++) {
                double delta = lambda[h] * entry.getValue().get(h - 1);
                updateHopList.set(h, updateHopList.get(h) + delta);
            }
            reachabilityMatrix.put(entry.getKey(), updateHopList);
        }
        mBLEHandler.updateHERAMatrixUI();
    }
    private void ageMatrix() {
        ;
        for (Map.Entry<String, List<Double>> entry :reachabilityMatrix.entrySet()) {
            List<Double> curList = entry.getValue();
            for (int i = 0; i < H; i++) {
                curList.set(i, curList.get(i) * alpha);
            }
            reachabilityMatrix.put(entry.getKey(), curList);
        }
        mBLEHandler.updateHERAMatrixUI();
    }
}