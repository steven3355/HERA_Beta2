package test.research.sjsu.hera_beta_version2;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Steven on 3/13/2018.
 */

public class Message {
    private String _destination;
    private byte[] _data;
    public Message(String destination, byte[] data) {
        _destination = destination;
        _data = data;
    }

    public byte[] getData() {
        return _data;
    }
    public byte[] getByte() {
        List<Byte> list = new ArrayList<>();
        for (int i = 0; i < _destination.length(); i++) {
            list.add((byte) _destination.charAt(i));
        }
        for (int i = 0; i < _data.length; i++) {
            list.add(_data[i]);
        }
        byte[] res = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = list.get(i);
        }
        return res;
    }

    public String getDestination() {
        return _destination;
    }
}
