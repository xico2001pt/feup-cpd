package pt.up.fe.cpd.server.membership.tasks;

import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.*;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class MulticastMembershipSender implements Runnable {
    final private InetAddress multicastAddress;
    final private int multicastPort;
    final private int membershipCounter;
    final private ClusterViewer clusterViewer;
    final private NodeInfo nodeInfo;

    public MulticastMembershipSender(InetAddress multicastAddress, int multicastPort, int membershipCounter, ClusterViewer clusterViewer, NodeInfo nodeInfo){
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.membershipCounter = membershipCounter;
        this.clusterViewer = clusterViewer;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public void run() {
        while(clusterViewer.getConnectionStatus() == ConnectionStatus.CONNECTED){
            try {
                TimeUnit.SECONDS.sleep(clusterViewer.getNodeCount());
            } catch (InterruptedException e) {
                return;
            }

            MembershipMessenger message = new MembershipMessenger(MembershipEvent.MEMBERSHIP, membershipCounter, multicastAddress, multicastPort);
            try {
                message.send(nodeInfo.getAddress() + " " + nodeInfo.getPort(), clusterViewer.getLogRepresentation());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}