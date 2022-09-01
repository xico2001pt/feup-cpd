package pt.up.fe.cpd.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ActiveNodeInfo extends NodeInfo {
    InetAddress inetAddress;

    public ActiveNodeInfo(String address, int storagePort) throws UnknownHostException {
        super(address, storagePort);
        this.inetAddress = InetAddress.getByName(address);
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }
}
