package pt.up.fe.cpd.server.replication;

import pt.up.fe.cpd.networking.TCPMessenger;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.utils.HashUtils;
import java.io.IOException;
import java.net.InetAddress;

public class SendDeleteRangeMessage implements Runnable {
    private final NodeInfo targetNode;
    private final byte[] lowestKey;
    private final byte[] highestKey;

    public SendDeleteRangeMessage(NodeInfo targetNode, byte[] lowestKey, byte[] highestKey){
        this.targetNode = targetNode;
        this.lowestKey = lowestKey;
        this.highestKey = highestKey;
    }

    @Override
    public void run(){
        InetAddress address;
        String message = "REPLICATE DELETE_RANGE " + HashUtils.keyByteToString(lowestKey) + " " + HashUtils.keyByteToString(highestKey);
        System.out.println("Deleting range ]" + HashUtils.keyByteToString(lowestKey).substring(0,5) + ", " + HashUtils.keyByteToString(highestKey).substring(0,5)  + "] in " + targetNode);
        try{
            address = InetAddress.getByName(targetNode.getAddress());
            TCPMessenger messenger = new TCPMessenger(address, targetNode.getPort());
            messenger.send(message.getBytes("UTF-8"));
        } catch(IOException e){
            e.printStackTrace();
            return;
        }        
    }
}
