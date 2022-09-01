package pt.up.fe.cpd.server.replication;

import pt.up.fe.cpd.networking.TCPMessenger;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.utils.HashUtils;
import java.io.IOException;
import java.net.InetAddress;

public class SendDeleteMessage implements Runnable {
    private final NodeInfo targetNode;
    private final byte[] key;

    public SendDeleteMessage(NodeInfo targetNode, byte[] key){
        this.targetNode = targetNode;
        this.key = key;
    }

    @Override
    public void run(){
        InetAddress address;
        String message = "REPLICATE DELETE " + HashUtils.keyByteToString(key);
        System.out.println("Delete" + HashUtils.keyByteToString(key).substring(0,5) + " in " + targetNode);
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
