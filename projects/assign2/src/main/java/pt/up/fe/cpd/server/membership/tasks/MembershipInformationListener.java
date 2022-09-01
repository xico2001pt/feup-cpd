package pt.up.fe.cpd.server.membership.tasks;

import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.cluster.ClusterManager;
import pt.up.fe.cpd.server.membership.log.MembershipLogEntry;

import java.io.IOException;
import java.util.concurrent.Callable;

public class MembershipInformationListener implements Callable<Boolean> {
    final private ActiveNodeInfo nodeInfo;
    final private ClusterManager clusterManager;
    private int neededConnections;

    public MembershipInformationListener(ActiveNodeInfo nodeInfo, ClusterManager clusterManager, int neededConnections){
        this.nodeInfo = nodeInfo;
        this.clusterManager = clusterManager;
        this.neededConnections = neededConnections;
    }

    @Override
    public Boolean call(){
        TCPListener listener;
        try {
            listener = new TCPListener(nodeInfo.getInetAddress(), nodeInfo.getPort() + 1, 1000);
        } catch (IOException e) {
            return false;
        }

        int connectionAttempts = this.neededConnections;
        for(int i = 0; i < connectionAttempts; ++i){
            try {
                String message = listener.receive();

                String[] splitMessage = message.split("\n");
                if(!splitMessage[0].equals("MEMBERSHIP")){
                    return false;
                }

                String[] nodeListInfo = splitMessage[2].split(", ");
                for(String nodeInfo : nodeListInfo){
                    String[] splitNodeInfo  = nodeInfo.split(":");
                    String receivedAddress  = splitNodeInfo[0];
                    int receivedPort        = Integer.parseInt(splitNodeInfo[1]);
                    clusterManager.addNode(new NodeInfo(receivedAddress, receivedPort));
                }

                String[] logInfo = splitMessage[3].split(", ");
                for(String logData : logInfo){
                    String[] splitLog               = logData.split(" ");
                    String[] splitNodeId            = splitLog[0].split(":");
                    String receivedAddress          = splitNodeId[0];
                    int receivedPort                = Integer.parseInt(splitNodeId[1]);
                    int receivedMembershipCounter   = Integer.parseInt(splitLog[1]);
                    clusterManager.addLogEntry(new MembershipLogEntry(receivedAddress, receivedPort, receivedMembershipCounter));
                }
                this.neededConnections--;
            } catch(IOException e) {
                listener.close();
                return false;
            }
        }

        listener.close();
        return this.neededConnections == 0;
    }
}