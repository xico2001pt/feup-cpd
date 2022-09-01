package pt.up.fe.cpd.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;

import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.membership.*;
import pt.up.fe.cpd.server.membership.cluster.ClusterManager;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.membership.cluster.SearchableCluster;
import pt.up.fe.cpd.server.membership.tasks.MembershipInformationListener;
import pt.up.fe.cpd.server.membership.tasks.MulticastListener;
import pt.up.fe.cpd.server.membership.tasks.MulticastMembershipSender;
import pt.up.fe.cpd.server.replication.RemoveFiles;
import pt.up.fe.cpd.utils.HashUtils;

public abstract class Node extends ActiveNodeInfo implements MembershipService {
    final private SearchableCluster cluster;
    final private InetAddress multicastAddress;
    final private int multicastPort;
    
    private int membershipCounter;
    final private ExecutorService executor;   // ThreadPool

    private TCPListener listener;

    public Node(String multicastAddress, int multicastPort, String address, int storagePort) throws UnknownHostException {
        super(address, storagePort);
        this.cluster = new SearchableCluster((ActiveNodeInfo) this, new File("membership_" + HashUtils.keyByteToString(this.getNodeId())));
        this.multicastAddress = InetAddress.getByName(multicastAddress);
        this.multicastPort = multicastPort;


        this.membershipCounter = 0;
        this.readMembershipCounter();
        if(this.membershipCounter % 2 != 0){
            this.membershipCounter += 1;
            this.saveMembershipCounter();
        }

        this.executor = Executors.newFixedThreadPool(8);
        printDebugInfo("Node started.");
    }

    protected ExecutorService getExecutor() {
        return this.executor;
    }

    protected TCPListener getListener() {
        return this.listener;
    }

    public SearchableCluster getCluster() {
        return cluster;
    }

    public InetAddress getMulticastAddress() {
        return multicastAddress;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void open(){
        try {
            this.listener = new TCPListener(this.getInetAddress(), this.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract void receive();

    public void close(){
        this.listener.close();
    }

    public void join() {
        Connection connection = cluster.getConnection();
        synchronized (connection){
            if (connection.getStatus() != ConnectionStatus.DISCONNECTED) return;
            connection.setStatus(ConnectionStatus.CONNECTING);
        }
        
        printDebugInfo("Joining the cluster");
        MembershipMessenger message = new MembershipMessenger(MembershipEvent.JOIN, this.membershipCounter, this.multicastAddress, this.multicastPort);
        try {
            MembershipInformationListener listener = new MembershipInformationListener((ActiveNodeInfo) this, cluster, 3);
            for (int i = 0; i < 3; ++i) {
                Future<Boolean> futureResult = executor.submit(listener);
                message.send(this.getAddress(), this.getPort());
                printDebugInfo("JOIN multicast message sent (" + (i+1) + "/3)");
                Boolean joinedSuccessfully = false;
                try {
                    joinedSuccessfully = futureResult.get();
                } catch (InterruptedException | ExecutionException e){
                    e.printStackTrace();
                }

                if (joinedSuccessfully) {
                    break;
                }
            }

            cluster.saveLog();
            printDebugInfo("Opening TCP connection");
            this.open();
            synchronized (connection){
                connection.setStatus(ConnectionStatus.CONNECTED);
            }
            this.receive();

            message = new MembershipMessenger(MembershipEvent.JOINED, this.membershipCounter, this.multicastAddress, this.multicastPort);
            message.send(this.getAddress(), this.getPort());
            printDebugInfo("Sent JOINED message");

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.cluster.registerJoinNode(this, this.membershipCounter);
        this.saveMembershipCounter();
        this.membershipCounter++;
        executor.execute(new MulticastListener(this, multicastAddress, multicastPort, (ClusterViewer) cluster, (ClusterManager) cluster, (ClusterSearcher) cluster, executor));
        executor.execute(new MulticastMembershipSender(multicastAddress, multicastPort, membershipCounter, (ClusterViewer) cluster, this));
    }

    public void leave() {
        printDebugInfo("Leaving the cluster");

        Connection connection = cluster.getConnection();
        synchronized (connection){
            if (connection.getStatus() != ConnectionStatus.CONNECTED) return;
            connection.setStatus(ConnectionStatus.DISCONNECTING);
        }
        
        MembershipMessenger message = new MembershipMessenger(MembershipEvent.LEAVE, this.membershipCounter, this.multicastAddress, this.multicastPort);
        try {
            message.send(this.getAddress(), this.getPort());
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        this.close();
        synchronized (connection){
            connection.setStatus(ConnectionStatus.DISCONNECTED);
        }

        this.cluster.registerLeaveNode(this, membershipCounter);
        this.saveMembershipCounter();
        this.membershipCounter++;

        executor.execute(new RemoveFiles(this, this.getNodeId(), this.getNodeId())); // Removes all the files
    }

    public String view(){
        StringBuilder builder = new StringBuilder();
        builder.append("Node: " + this.toString())
                .append("\nStatus: " + this.cluster.getConnectionStatus())
                .append("\nHash: " + HashUtils.keyByteToString(this.getNodeId()))
                .append("\nMembership Counter: " + (this.membershipCounter - 1))
                .append("\nLog:\n ")
                .append(cluster.getLogRepresentation())
                .append("\nNode set:\n ")
                .append(cluster.getNodeRepresentation())
                .append("\n");
        return builder.toString();
    }

    private void saveMembershipCounter(){
        File file = new File("membership_" + HashUtils.keyByteToString(this.getNodeId()) + "/membership.counter");
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(Integer.toString(this.membershipCounter).getBytes("UTF-8"));
            outputStream.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMembershipCounter(){
        File file = new File("membership_" + HashUtils.keyByteToString(this.getNodeId()) + "/membership.counter");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            this.membershipCounter = Integer.parseInt(reader.readLine()) + 1;
            reader.close();
        } catch(FileNotFoundException e){
            this.membershipCounter = 0;
        } catch(IOException e){
            this.membershipCounter = 0;
        }
    }

    protected void printDebugInfo(String message){
        System.out.println("[" + getAddress() + ":" + getPort()  + "] " + message);
    }
}
