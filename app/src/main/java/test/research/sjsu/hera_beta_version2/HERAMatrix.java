package test.research.sjsu.hera_beta_version2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Steven on 3/19/2018.
 */

public class HERAMatrix{
    private Map<String, List<Double>> _reachabilityMatrix;
    HERAMatrix(Map<String, List<Double>> map) {
        _reachabilityMatrix = map;
    }
    HERAMatrix() {
        _reachabilityMatrix = new HashMap<>();
    }

    public Map<String, List<Double>> getReachabilityMatrix() {
        return _reachabilityMatrix;
    }
    boolean containsKey(String dest) {
        return _reachabilityMatrix.containsKey(dest);
    }
    List<Double> get(String dest) {
        return _reachabilityMatrix.get(dest);
    }
    void put(String dest, List<Double> list) {
        _reachabilityMatrix.put(dest, list);
    }
    Set<Map.Entry<String, List<Double>>> entrySet() {
        return _reachabilityMatrix.entrySet();
    }
}
