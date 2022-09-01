package pt.up.fe.cpd.server.membership.cluster;

import java.io.File;

import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.utils.Pair;

public class SearchableCluster extends Cluster implements ClusterSearcher{
    private final ActiveNodeInfo activeNode;

    public SearchableCluster(ActiveNodeInfo activeNode, File directory) {
        super(directory);
        this.activeNode = activeNode;
    }

    @Override
    public NodeInfo findNodeByKey(byte[] key){
        NodeInfo item =  nodeSet.higher(new NodeInfo(key));
        if(item == null){ // Circular node representation
            return nodeSet.first();
        }
        return item;
    }

    @Override
    public Pair<NodeInfo, NodeInfo> findTwoClosestNodes(NodeInfo nodeInfo){
        NodeInfo lower = nodeSet.lower(nodeInfo);
        NodeInfo higher = nodeSet.higher(nodeInfo);
        if(lower == null){
            lower = nodeSet.last();
        }
        if(higher == null){
            higher = nodeSet.first();
        }
        return new Pair<>(lower, higher);
    }

    @Override
    public boolean isActiveNode(NodeInfo nodeInfo) {
        return nodeInfo.equals((NodeInfo) activeNode);
    }

    @Override
    public ActiveNodeInfo getActiveNode(){
        return activeNode;
    }
}
