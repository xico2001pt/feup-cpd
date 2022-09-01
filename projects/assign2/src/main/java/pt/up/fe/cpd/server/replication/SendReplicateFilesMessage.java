package pt.up.fe.cpd.server.replication;

import pt.up.fe.cpd.networking.FileTransfer;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.utils.HashUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class SendReplicateFilesMessage implements Runnable {
    private final NodeInfo currentNode;
    private final NodeInfo targetNode;
    private final byte[] lowestKey;
    private final byte[] highestKey;

    public SendReplicateFilesMessage(NodeInfo currentNode, NodeInfo targetNode, byte[] lowestKey, byte[] highestKey){
        this.currentNode = currentNode;
        this.targetNode = targetNode;
        this.lowestKey = lowestKey;
        this.highestKey = highestKey;
    }

    @Override
    public void run(){
        File directory = new File("store_" + HashUtils.keyByteToString(currentNode.getNodeId()));
        File[] matchingFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".tombstone")){
                    name = name.replace(".tombstone", "");
                }

                byte[] fileKey = HashUtils.keyStringToByte(name);

                int comparison = HashUtils.compare(lowestKey, highestKey);
                if(comparison == 0){
                    return true;
                } else if(comparison < 0){  //  lowestKey < highestKey
                    // lowestKey < fileKey <= highestKey
                    comparison = HashUtils.compare(fileKey, lowestKey);
                    if(comparison <= 0){
                        return false;
                    }
                    return HashUtils.compare(fileKey, highestKey) <= 0;
                } else { // lowestKey > highestKey
                    // fileKey in [0, highestKey[ U [lowestKey, +infinity[ 
                    Boolean greaterEqThanHighestKey = HashUtils.compare(fileKey, highestKey) >= 0;
                    Boolean lowerThanLowestKey = HashUtils.compare(fileKey, lowestKey) < 0;
                    return !(greaterEqThanHighestKey && lowerThanLowestKey);
                }
            }
        });

        if(matchingFiles != null){
            for (File file : matchingFiles) {
                this.sendReplicationOperation(file);
            }
        }
    }

    private void sendReplicationOperation(File file){
        System.out.println("[" + currentNode + "] replicating " + file.getName().substring(0, 5) + " to node " + targetNode);
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
