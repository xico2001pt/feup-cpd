package pt.up.fe.cpd.networking;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.net.*;

public class TCPMessenger extends NetworkMessenger {
    public TCPMessenger(InetAddress address, int port) {
        super(address, port);
    }

    @Override
    public void send(byte[] data) throws IOException {
        Socket socket = new Socket(address, port);
        OutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        outputStream.write(data);
        outputStream.flush();
        socket.close();
    }
}
