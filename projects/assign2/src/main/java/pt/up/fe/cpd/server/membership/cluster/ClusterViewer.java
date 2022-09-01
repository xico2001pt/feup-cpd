package pt.up.fe.cpd.server.membership.cluster;

import java.util.Set;

import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.ConnectionStatus;

public interface ClusterViewer {
    ConnectionStatus getConnectionStatus();
    String getLogRepresentation();
    String getNodeRepresentation();
    int getMembershipCounter(NodeInfo node);
    int getNodeCount();
    Set<NodeInfo> getNodeSet();
}
