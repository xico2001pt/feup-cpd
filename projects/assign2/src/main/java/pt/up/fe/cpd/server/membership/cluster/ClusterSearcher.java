package pt.up.fe.cpd.server.membership.cluster;

import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.utils.Pair;

public interface ClusterSearcher {
    NodeInfo findNodeByKey(byte[] key);
    Pair<NodeInfo, NodeInfo> findTwoClosestNodes(NodeInfo nodeInfo);
    boolean isActiveNode(NodeInfo nodeInfo);
    ActiveNodeInfo getActiveNode();
}
