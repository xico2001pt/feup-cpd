package pt.up.fe.cpd.server.store.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import pt.up.fe.cpd.networking.FileTransfer;
import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.MembershipEvent;
import pt.up.fe.cpd.server.membership.MembershipMessenger;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;
import pt.up.fe.cpd.server.replication.RemoveFiles;
import pt.up.fe.cpd.server.replication.SendReplicateFileMessage;
import pt.up.fe.cpd.server.replication.SendDeleteMessage;
import pt.up.fe.cpd.server.store.KeyValueStore;
import pt.up.fe.cpd.utils.HashUtils;
import pt.up.fe.cpd.utils.Pair;

public class StoreOperationHandler implements Runnable {
    final private KeyValueStore keyValueStore;
    final private Socket socket;
    final private ClusterSearcher searcher;
    final private ClusterViewer clusterViewer;
    final private ExecutorService executor;
    final private InetAddress multicastAddress;
    final private int multicastPort;
    
    public StoreOperationHandler(KeyValueStore keyValueStore, Socket socket, ClusterSearcher searcher, ClusterViewer clusterViewer, ExecutorService executor, InetAddress multicaAddress, int multicaPort) {
        this.keyValueStore = keyValueStore;
        this.socket = socket;
        this.searcher = searcher;
        this.clusterViewer = clusterViewer;
        this.executor = executor;
        this.multicastAddress = multicaAddress;
        this.multicastPort = multicaPort;
    }

    @Override
    public void run() {        
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            
            Scanner scanner = new Scanner(dataInputStream);
            String header = scanner.nextLine();

            String[] splitHeader = header.split(" ");
            String operation = splitHeader[0];
            
            // REPLICATE PUT key
            // REPLICATE DELETE key
            // REPLICATE DELETE_RANGE key1 key2
            if (operation.equals("REPLICATE")) {
                operation = splitHeader[1];
                System.out.println("Received a " + operation + " REPLICATION request.");
                if(operation.equals("DELETE_RANGE")){
                    handleDeleteRange(splitHeader[2], splitHeader[3]);
                } else {
                    String key = splitHeader[2];
                    handleReplicationRequest(operation, key, dataInputStream);    
                }
            } else {
                String key = splitHeader[1];
                NodeInfo node = this.searcher.findNodeByKey(HashUtils.keyStringToByte(key));
                if(this.searcher.isActiveNode(node)){
                    System.out.println("Key belongs to this node.");
                    handleRequest(operation, key, dataInputStream);
                } else {
                    System.out.println("Key belongs to node " + node.toString());
                    boolean sent = handleRedirect(node, operation, key, dataInputStream);
                    if(!sent){
                        handleRedirectToCrashedNode(node, operation, key, dataInputStream);
                    }
                }
            }

            scanner.close();
            dataInputStream.close();
            socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private void handleDeleteRange(String lowestKey, String highestKey) {
        System.out.println("Deleting range ]" + lowestKey.substring(0,5) + ", " + highestKey.substring(0,5)  + "]");
        ActiveNodeInfo activeNode = this.searcher.getActiveNode();
        RemoveFiles task = new RemoveFiles(activeNode, HashUtils.keyStringToByte(lowestKey), HashUtils.keyStringToByte(highestKey));
        task.run();
    }

    private void handleRequest(String operation, String key, DataInputStream dataInputStream) throws IOException {
        this.handleReplicationRequest(operation, key, dataInputStream);
        System.out.println("Received a " + operation + " request.");
        NodeInfo currentNode = this.searcher.getActiveNode();
        Pair<NodeInfo, NodeInfo> neighbours = searcher.findTwoClosestNodes(currentNode);

        switch(operation){
            case "PUT":
                // Replicate files to the two adjacent nodes
                if(!neighbours.first.equals(currentNode)){ // More than 1 node in the cluster
                    executor.execute(new SendReplicateFileMessage(currentNode, neighbours.first, HashUtils.keyStringToByte(key)));
                    if(!neighbours.first.equals(neighbours.second)){ // If there's only 2 nodes the neighbours will be the same node
                        executor.execute(new SendReplicateFileMessage(currentNode, neighbours.second, HashUtils.keyStringToByte(key)));
                    }
                }
                break;
            case "DELETE":
                // Replicate tombstone files to the two adjacent neighbours
                if(!neighbours.first.equals(currentNode)){ // More than 1 node in the cluster
                    executor.execute(new SendDeleteMessage(neighbours.first, HashUtils.keyStringToByte(key)));
                    if(!neighbours.first.equals(neighbours.second)){ // If there's only 2 nodes the neighbours will be the same node
                        executor.execute(new SendDeleteMessage(neighbours.second, HashUtils.keyStringToByte(key)));
                    }
                }
                break;
        }
    }

    private void handleReplicationRequest(String operation, String key, DataInputStream dataInputStream) throws IOException {
        switch(operation) {
            case "GET":
                DataOutputStream dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
                keyValueStore.get(key, dataOutputStream);
                dataOutputStream.close();
                break;
            case "DELETE":
                keyValueStore.delete(key);
                break;
            case "PUT":
                keyValueStore.put(key, dataInputStream);
                break;
        }
    }

    private boolean handleRedirect(NodeInfo node, String operation, String key, DataInputStream clientInputStream) throws IOException {
        InetAddress nodeAddress = InetAddress.getByName(node.getAddress());

        Socket nodeSocket;
        try {
            nodeSocket = new Socket(nodeAddress, node.getPort());
        } catch(ConnectException e){
            System.out.println("Connection to " + node + " refused.");
            MembershipMessenger message = new MembershipMessenger(MembershipEvent.LEAVE, this.clusterViewer.getMembershipCounter(node)+1, this.multicastAddress, this.multicastPort);
            message.send(node.getAddress(), node.getPort());
            return false;
        }
        
        DataOutputStream nodeOutputStream = new DataOutputStream(nodeSocket.getOutputStream());

        nodeOutputStream.write((operation + " " + key + "\n").getBytes("UTF-8"));
        switch(operation) {
            case "GET":
                DataOutputStream clientOutputStream = new DataOutputStream(socket.getOutputStream());
                DataInputStream nodeInputStream = new DataInputStream(nodeSocket.getInputStream());
                FileTransfer.transfer(nodeInputStream, clientOutputStream);
                nodeInputStream.close();
                clientOutputStream.close();
                break;
            case "PUT":
                nodeOutputStream.write(("\n").getBytes("UTF-8"));
                FileTransfer.transfer(clientInputStream, nodeOutputStream);
                break;
        }
        
        nodeSocket.close();
        return true;
    }

    private boolean handleRedirectToCrashedNode(NodeInfo node, String operation, String key, DataInputStream clientInputStream) throws IOException {
        switch(operation){
            case "GET":
                Pair<NodeInfo, NodeInfo> neighbours = this.searcher.findTwoClosestNodes(node);
                if(!this.handleRedirect(neighbours.first, operation, key, clientInputStream)) {
                    return this.handleRedirect(neighbours.second, operation, key, clientInputStream);
                }
                return true;
            case "PUT":
            case "DELETE":
                for(int i = 0; i < 3; ++i){
                    NodeInfo keyOwnerNode = this.searcher.findNodeByKey(HashUtils.keyStringToByte(key));
                    boolean sent = this.handleRedirect(keyOwnerNode, operation, key, clientInputStream);
                    if(sent) {
                        return true;
                    }
                    try {
                        TimeUnit.SECONDS.sleep(1);    
                    } catch(InterruptedException e){}
                }
                return false;
        }
        return false;
    }
}