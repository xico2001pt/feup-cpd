package pt.up.fe.cpd.server.replication;

import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.utils.HashUtils;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class RemoveFiles implements Runnable {
    private final NodeInfo currentNode;
    private final byte[] lowestKey;
    private final byte[] highestKey;

    public RemoveFiles(NodeInfo currentNode, byte[] lowestKey, byte[] highestKey){
        this.currentNode = currentNode;
        this.lowestKey = lowestKey;
        this.highestKey = highestKey;
    }

    @Override
    public void run(){
        File directory = new File("store_" + HashUtils.keyByteToString(currentNode.getNodeId()));

        File[] matchingFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if(name.endsWith(".tombstone")){
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
                file.delete();
            }
        }
    }
}
