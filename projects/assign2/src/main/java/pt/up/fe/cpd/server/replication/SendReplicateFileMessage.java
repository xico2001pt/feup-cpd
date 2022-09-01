package pt.up.fe.cpd.server.replication;

import pt.up.fe.cpd.networking.FileTransfer;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.utils.HashUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class SendReplicateFileMessage implements Runnable {
    private final NodeInfo currentNode;
    private final NodeInfo targetNode;
    private final byte[] key;

    public SendReplicateFileMessage(NodeInfo currentNode, NodeInfo targetNode, byte[] key){
        this.currentNode = currentNode;
        this.targetNode = targetNode;
        this.key = key;
    }

    @Override
    public void run(){
        File file = new File("store_" + HashUtils.keyByteToString(currentNode.getNodeId()) + "/" + HashUtils.keyByteToString(key));
        if(!file.exists()){
            return;
        }

        System.out.println("["+ currentNode + "] replicating " + file.getName().substring(0, 5) + " to node " + targetNode);

        try {
            InetAddress targetNodeAddress = InetAddress.getByName(targetNode.getAddress());
            Socket socket = new Socket(targetNodeAddress, targetNode.getPort());

            FileInputStream fileInputStream = new FileInputStream(file);

            DataInputStream inputStream = new DataInputStream(fileInputStream);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            outputStream.write(("REPLICATE PUT " + file.getName() + "\n\n").getBytes("UTF-8"));
            boolean transferSuccessful = FileTransfer.transfer(inputStream, outputStream);

            inputStream.close();
            outputStream.close();
            socket.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
