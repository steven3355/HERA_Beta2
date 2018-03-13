package test.research.sjsu.hera_beta_version2;

import android.util.Log;

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

/**
 * Created by Steven on 3/13/2018.
 */

public class HERA implements Serializable {

    static final int DEFAULT_HOPS_SIZE = 5;
    static final double[] DEFAULT_LAMBDA = {1, .5, .05, .005, .0005};
    static final double[] DEFAULT_GAMMA = {1, .5, .05, .005, .0005};
    static final double DEFAULT_ALPHA = 0.98;

    private int H = 5;
    private double[] lambda = {1, 0.5, 0.05, 0.005, 0.0005},
            gamma = {1, 0.5, 0.05, 0.005, 0.0005};
    private double alpha = 0.98;
    private Map<String, List<Double>> reachabilityMatrix;
    private String self = android_id;
    private static ScheduledExecutorService matrixAger = Executors.newSingleThreadScheduledExecutor();
    private int ageTime;
    private String TAG = "HERA";
    public HERA() {
        H = DEFAULT_HOPS_SIZE;
        lambda = DEFAULT_LAMBDA;
        gamma = DEFAULT_GAMMA;
        alpha = DEFAULT_ALPHA;
        reachabilityMatrix = new HashMap<>();
        ageTime = 10;
        matrixAger.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        ageMatrix();
                        Log.d(TAG, "Hera matrix aged");
                    }
                }
                , 0, ageTime, TimeUnit.SECONDS
        );
    }
    public HERA(Map<String, List<Double>> neighborMatrix) {
        reachabilityMatrix = neighborMatrix;
    }
    public HERA(int maxHop, double agingConstant, double[] intrinsicConfidence, double[] weight, int timeUnit, int agingTime, String selfAddress) {
        H = maxHop;
        alpha = agingConstant;
        lambda = intrinsicConfidence;
        gamma = weight;
        ageTime = agingTime;
        self = selfAddress;
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
            reachabilityMatrix.put(dest, new ArrayList<>(Collections.nCopies(H, 0.0)));
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
    }
    public void updateTransitiveHops(String neighbor, Map<String, List<Double>> neighborMap) {
        for(Map.Entry<String, List<Double>> entry : neighborMap.entrySet()) {
            if (entry.getKey().equals(self)) {
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
    }
    private void ageMatrix() {
        double multiplier = Math.pow(alpha, ageTime);
        for (Map.Entry<String, List<Double>> entry :reachabilityMatrix.entrySet()) {
            List<Double> curList = entry.getValue();
            for (int i = 0; i < H; i++) {
                curList.set(i, curList.get(i) * multiplier);
            }
            reachabilityMatrix.put(entry.getKey(), curList);
        }
    }
}