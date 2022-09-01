package pt.up.fe.cpd.server.membership.tasks;

import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.cluster.ClusterManager;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;
import pt.up.fe.cpd.server.membership.ConnectionStatus;
import pt.up.fe.cpd.server.membership.MembershipEvent;
import pt.up.fe.cpd.server.membership.MembershipMessenger;
import pt.up.fe.cpd.server.membership.log.MembershipLogEntry;
import pt.up.fe.cpd.server.replication.RemoveFiles;
import pt.up.fe.cpd.server.replication.SendReplicateFilesMessage;
import pt.up.fe.cpd.server.replication.SendDeleteRangeMessage;
import pt.up.fe.cpd.utils.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class MulticastListener implements Runnable {
    final private InetAddress multicastAddress;
    final private int multicastPort;
    final private ActiveNodeInfo nodeInfo;
    final private ClusterViewer clusterViewer;
    final private ClusterManager clusterManager;
    final private ClusterSearcher clusterSearcher;
    final private ExecutorService executor;
    final private Map<NodeInfo, Integer> nodeTimeToLiveMap;

    public MulticastListener(ActiveNodeInfo nodeInfo, InetAddress multicastAddress, int multicastPort,
                             ClusterViewer clusterViewer, ClusterManager clusterManager, ClusterSearcher clusterSearcher,
                             ExecutorService executor) {
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.nodeInfo = nodeInfo;
        this.clusterViewer = clusterViewer;
        this.clusterManager = clusterManager;
        this.clusterSearcher = clusterSearcher;
        this.executor = executor;
        this.nodeTimeToLiveMap = new HashMap<>();
        this.clusterViewer.getNodeSet().forEach(n -> this.nodeTimeToLiveMap.put(n, clusterViewer.getNodeCount() * 3));
    }

    @Override
    public void run() {
        MulticastSocket socket;
        try {
            socket = new MulticastSocket(multicastPort);
            socket.joinGroup(multicastAddress);
        } catch(IOException e) {
            System.out.println("IO Exception");
            e.printStackTrace();
            return;
        }

        byte[] buf = new byte[2048];
        while (clusterViewer.getConnectionStatus() == ConnectionStatus.CONNECTED) {
            DatagramPacket packet = new DatagramPacket(buf, 2048);
            try {
                socket.receive(packet);
            } catch(IOException e) {
                socket.close();
                e.printStackTrace();
                return;
            }

            String received = new String(packet.getData(), 0, packet.getLength());
            String[] splitString = received.split(" ");
            String eventType = splitString[0];

            switch(eventType){
                case "JOIN": // Joining
                    System.out.println("[" + this.nodeInfo +"] Received JOIN multicast message");
                    handleJoin(received);
                    break;
                case "LEAVE": // Leaving
                    System.out.println("[" + this.nodeInfo +"] Received LEAVE multicast message");
                    handleLeave(received);
                    break;
                case "MEMBERSHIP":
                    handleMembership(received);
                    break;
                case "JOINED":  // Node joined cluster successfully
                    System.out.println("[" + this.nodeInfo +"] Received JOINED multicast message");
                    handleJoined(received);
                    break;
            }
        }

        try {
            socket.leaveGroup(multicastAddress);
        } catch(IOException e) {
            socket.close();
            e.printStackTrace();
            return;
        }

        socket.close();
    }

    private Pair<ActiveNodeInfo, Integer> parseMessageHeader(String receivedData) throws UnknownHostException {
        String[] splitString = receivedData.split(" ");
        String receivedAddress = splitString[1];
        int receivedPort = Integer.parseInt(splitString[2]);
        int receivedCounter = Integer.parseInt(splitString[3]);
        return new Pair<>(new ActiveNodeInfo(receivedAddress, receivedPort), receivedCounter);
    }

    private void handleJoin(String receivedData){
        Pair<ActiveNodeInfo, Integer> parsedData;
        try {
            parsedData = parseMessageHeader(receivedData);
        } catch (IOException e){
            e.printStackTrace();
            return;
        }

        ActiveNodeInfo parsedNodeInfo = parsedData.first;
        if (parsedNodeInfo.getAddress().equals(this.nodeInfo.getAddress()) &&
                parsedNodeInfo.getPort() == this.nodeInfo.getPort()) {
            return;
        }

        boolean updatedLog = clusterManager.addLogEntry(new MembershipLogEntry(parsedNodeInfo.getAddress(), 
                                                                               parsedNodeInfo.getPort(), parsedData.second));
        if(updatedLog){     // Node already sent MEMBERSHIP information
            clusterManager.saveLog();
            executor.execute(new MembershipInformationSender(parsedNodeInfo, clusterViewer));
        }
    }

    private void handleJoined(String receivedData){
        Pair<ActiveNodeInfo, Integer> parsedData;
        try {
            parsedData = parseMessageHeader(receivedData);
        } catch (IOException e){
            e.printStackTrace();
            return;
        }

        ActiveNodeInfo parsedNodeInfo = parsedData.first;
        int receivedCounter = parsedData.second;

        if (parsedNodeInfo.getAddress().equals(this.nodeInfo.getAddress()) &&
                parsedNodeInfo.getPort() == this.nodeInfo.getPort()) {
            return;
        }

        this.joinCluster(parsedNodeInfo, receivedCounter);        
    }

    private void joinCluster(NodeInfo newNodeInfo, int membershipCounter){
        this.nodeTimeToLiveMap.put(newNodeInfo, this.clusterViewer.getNodeCount() * 3);
        Pair<NodeInfo, NodeInfo> oldNeighbours = this.clusterSearcher.findTwoClosestNodes(this.nodeInfo);
        // Replication "transaction"
        synchronized(clusterManager){
            clusterManager.registerJoinNode(newNodeInfo, membershipCounter);
            Pair<NodeInfo, NodeInfo> newNeighbours = clusterSearcher.findTwoClosestNodes(this.nodeInfo);
            
            System.out.println(nodeInfo + " handling join,");

            if(!oldNeighbours.first.equals(newNeighbours.first)){
                Pair<NodeInfo, NodeInfo> bNeighbours = clusterSearcher.findTwoClosestNodes(oldNeighbours.first);
                NodeInfo ANode = bNeighbours.first;
                NodeInfo BNode = oldNeighbours.first;
                NodeInfo CNode = newNeighbours.first;
                NodeInfo DNode = this.nodeInfo;
                NodeInfo ENode = oldNeighbours.second;
                
                // Send files ]B,D]
                executor.execute(new SendReplicateFilesMessage(DNode, newNodeInfo, BNode.getNodeId(), DNode.getNodeId()));

                if(clusterViewer.getNodeCount() > 3){
                    // Remove ]A,B]
                    executor.execute(new RemoveFiles(DNode, ANode.getNodeId(), BNode.getNodeId()));

                    // (On Node E) Remove ]B,C]  
                    System.out.println(this.nodeInfo + " --> (" + ENode + "," + BNode + "," + CNode + ")");                
                    executor.execute(new SendDeleteRangeMessage(ENode, BNode.getNodeId(), CNode.getNodeId()));
                }
            }

            if(!oldNeighbours.second.equals(newNeighbours.second) && clusterViewer.getNodeCount() > 2){ // It's redundant checking if nodeCount == 2, since both 'sides' are the equal
                NodeInfo ANode = oldNeighbours.first;
                NodeInfo BNode = this.nodeInfo;
                NodeInfo CNode = newNeighbours.second;
                NodeInfo DNode = oldNeighbours.second;

                // Send files ]A,B] to C
                executor.execute(new SendReplicateFilesMessage(BNode, CNode, ANode.getNodeId(), BNode.getNodeId()));
                
                if(clusterViewer.getNodeCount() > 3){
                    // Remove files ]C,D] 
                    executor.execute(new RemoveFiles(BNode, CNode.getNodeId(), DNode.getNodeId()));
                }
            }
        }
    }
    
    private void handleLeave(String receivedData){
        Pair<ActiveNodeInfo, Integer> parsedData;
        try {
            parsedData = parseMessageHeader(receivedData);
        } catch(IOException e){
            e.printStackTrace();
            return;
        }

        NodeInfo parsedNodeInfo = parsedData.first;
        int receivedCounter = parsedData.second;

        if (parsedNodeInfo.getAddress().equals(this.nodeInfo.getAddress()) &&
                parsedNodeInfo.getPort() == this.nodeInfo.getPort()) {
            return;
        }

        boolean updatedLog = clusterManager.addLogEntry(new MembershipLogEntry(parsedNodeInfo.getAddress(), 
                                                                               parsedNodeInfo.getPort(), parsedData.second));
        if(updatedLog){
            clusterManager.saveLog();
        }

        this.leaveCluster(parsedNodeInfo, receivedCounter);
    }
    
    private void leaveCluster(NodeInfo oldNode, int membershipCounter) {
        this.nodeTimeToLiveMap.remove(oldNode);
        Pair<NodeInfo, NodeInfo> oldNeighbours = clusterSearcher.findTwoClosestNodes(this.nodeInfo);

        // Replication "transaction"
        synchronized(clusterManager){
            clusterManager.registerLeaveNode(oldNode, membershipCounter);
            Pair<NodeInfo, NodeInfo> newNeighbours = clusterSearcher.findTwoClosestNodes(this.nodeInfo);
            
            System.out.println(nodeInfo + " handling leave,");

            if(!oldNeighbours.first.equals(newNeighbours.first)){
                // This is Node D
                NodeInfo BNode = newNeighbours.first;
                NodeInfo CNode = oldNeighbours.first;
                NodeInfo DNode = this.nodeInfo;
                NodeInfo ENode = newNeighbours.second;

                // Send ]C,D] to B node
                executor.execute(new SendReplicateFilesMessage(DNode, BNode, CNode.getNodeId(), DNode.getNodeId()));

                // Send ]B,C] to E node
                executor.execute(new SendReplicateFilesMessage(DNode, ENode, BNode.getNodeId(), CNode.getNodeId()));           
            }

            if(!oldNeighbours.second.equals(newNeighbours.second)){
                // This is Node B
                NodeInfo ANode = oldNeighbours.first;
                NodeInfo BNode = this.nodeInfo;
                NodeInfo DNode = newNeighbours.second;

                // Send ]A,B] to D node
                executor.execute(new SendReplicateFilesMessage(BNode, DNode, ANode.getNodeId(), BNode.getNodeId()));
            }
        }
    }

    private void handleMembership(String receivedData){
        String[] splitMessage = receivedData.split("\n");

        String[] splitHeader = splitMessage[0].split(" ");
        String senderAddress = splitHeader[1];
        int senderPort = Integer.parseInt(splitHeader[2]);

        String[] logInfo = splitMessage[2].split(", ");
        boolean anyLogUpdate = false;
        for(String logData : logInfo){
            String[] splitLog = logData.split(" ");
            String[] splitNodeId = splitLog[0].split(":");
            String entryAddress = splitNodeId[0];
            int entryPort = Integer.parseInt(splitNodeId[1]);
            int entryMembershipCounter = Integer.parseInt(splitLog[1]);
            boolean updated = clusterManager.addLogEntry(new MembershipLogEntry(entryAddress, entryPort, entryMembershipCounter));
            if(updated){
                anyLogUpdate = true;
                NodeInfo newNodeInfo = new NodeInfo(entryAddress, entryPort);
                if(entryMembershipCounter % 2 == 0){
                    this.joinCluster(newNodeInfo, entryMembershipCounter);
                } else {
                    this.leaveCluster(newNodeInfo, entryMembershipCounter);
                }
            }
        }

        this.updateTimeToLive(new NodeInfo(senderAddress, senderPort));

        if(anyLogUpdate){
            this.clusterManager.saveLog();
        }
    }
    
    private void updateTimeToLive(NodeInfo info){
        for(Map.Entry<NodeInfo, Integer> entry : this.nodeTimeToLiveMap.entrySet()){
            if(entry.getValue() <= 1 && !entry.getKey().equals(info)){
                NodeInfo n = entry.getKey();
                System.out.println(n + " has been unresponsive for too long, sending LEAVE.");
                MembershipMessenger message = new MembershipMessenger(MembershipEvent.LEAVE, this.clusterViewer.getMembershipCounter(n)+1, this.multicastAddress, this.multicastPort);
                try {
                    message.send(n.getAddress(), n.getPort());
                } catch(IOException e){
                    e.printStackTrace();
                }
            } else {
                this.nodeTimeToLiveMap.put(entry.getKey(), entry.getValue() - 1);
            }
        }
        if(this.nodeTimeToLiveMap.containsKey(info)){
            this.nodeTimeToLiveMap.put(info, this.clusterViewer.getNodeCount() * 3);
        }
    }
}