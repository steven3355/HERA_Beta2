package test.research.sjsu.hera_beta_version2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static test.research.sjsu.hera_beta_version2.MainActivity.android_id;
import static test.research.sjsu.hera_beta_version2.MainActivity.mHera;

/**
 * MessageSystem
 * Keeps track of all the message that this device has waiting to be sent
 * Created by Steven on 3/13/2018.
 */

public class MessageSystem {
    private Map<String, Queue<Message>> messageMap;
    private static String TAG = "MessageSystem";
    MessageSystem() {
        messageMap = new HashMap<>();
        generateTestMessageForClients();

    }
    Queue<Message> getMessageQueue(String dest) {
        return messageMap.get(dest);
    }
    void putMessage(byte[] input) {
        String dest = new String(Arrays.copyOfRange(input, 0, 16));
        byte[] data = Arrays.copyOfRange(input, 16, input.length);
        if (!messageMap.containsKey(dest)) {
            messageMap.put(dest, new LinkedList<Message>());
        }
        messageMap.get(dest).add(new Message(dest, data));
    }
    boolean hasMessage(String dest) {
        return getMessageQueue(dest) != null && !getMessageQueue(dest).isEmpty();
    }
    public Message getMessage(String dest) {
        return getMessageQueue(dest).peek();
    }
    public void  MessageAck(Connection curConnection) {
        String dest = curConnection.getOneToSendDestination();
        getMessageQueue(dest).remove();
        if (!hasMessage(dest)) {
            curConnection.toSendDestinationUpdate();
            messageMap.remove(dest);
        }
    }
    List<String> getMessageDestinationList() {
        return new ArrayList<>(messageMap.keySet());
    }

    public void buildToSendMessageQueue(Connection curConnection) {
        HERAMatrix neighborHera = curConnection.getNeighborHERAMatrix();
        List<String> mMessageDestinationList = this.getMessageDestinationList();

        for (String dest : mMessageDestinationList) {
            if (!hasMessage(dest)) {
                messageMap.remove(dest);
                continue;
            }
            if (dest.equals(android_id)) {
                continue;
            }
            if (mHera.makeDecision(dest, neighborHera) || dest.equals(curConnection.getNeighborAndroidID())) {
                curConnection.pushToSendQueue(dest);
            }
        }
    }
    public void generateTestMessage(String dest, int count) {
        messageMap.put(dest, new LinkedList<Message>());
        for (int i = 0; i < count; i++) {
            messageMap.get(dest).add(new Message(dest, ("this is test message #" + i).getBytes()));
        }
    }
    public void generateTestMessageForClients() {
        switch(android_id) {
            case "8ab5e5cf2db6867a":
                generateTestMessage("a1417d377e1333ac", 10);
                break;
            case "a1417d377e1333ac":
                generateTestMessage("8ab5e5cf2db6867a", 10);
                break;
        }
    }
    public int getMessageSystemSize() {
        int count = 0;
        for (String dest : getMessageDestinationList()) {
            count += messageMap.get(dest).size();
        }
        return count;
    }

    Map<String, Queue<Message>> getMessageMap() {
        return messageMap;
    }
}
