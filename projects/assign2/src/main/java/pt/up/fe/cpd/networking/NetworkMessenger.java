package pt.up.fe.cpd.networking;

import java.io.IOException;
import java.net.InetAddress;

public abstract class NetworkMessenger {
    protected InetAddress address;
    protected int port;

    public NetworkMessenger(InetAddress address, int port){
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public abstract void send(byte[] data) throws IOException;
}
