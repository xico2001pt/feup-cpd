package pt.up.fe.cpd.networking;

import java.io.IOException;
import java.net.*;

public class MulticastMessenger extends NetworkMessenger {
    public MulticastMessenger(InetAddress address, int port) {
        super(address, port);
    }

    @Override
    public void send(byte[] data) throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(address);

        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
        socket.close();
    }
}

