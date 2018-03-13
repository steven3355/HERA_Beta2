package test.research.sjsu.hera_beta_version2;

/**
 * Created by Steven on 3/13/2018.
 */

public class Message {
    private String _destination;
    private byte[] _data;
    public Message(byte[] data) {
        _data = data;
    }

    public byte[] getByte() {
        return _data;
    }

    public String getDestination() {
        return _destination;
    }
}
