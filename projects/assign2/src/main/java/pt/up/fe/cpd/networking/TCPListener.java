package pt.up.fe.cpd.networking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.*;
import java.util.stream.Collectors;

public class TCPListener {
    private ServerSocket serverSocket;

    public TCPListener(InetAddress address, int port) throws IOException {
        this.serverSocket = new ServerSocket(port, 0, address);
    }

    public TCPListener(InetAddress address, int port, int timeout) throws IOException {
        this.serverSocket = new ServerSocket(port, 0, address);
        this.serverSocket.setSoTimeout(timeout);
    }

    public void close() {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            return;
        }
    }

    public Socket accept() throws IOException {
        return this.serverSocket.accept();
    }

    public String receive() throws IOException {
        Socket socket = this.accept();
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String input = inputStream.lines().collect(Collectors.joining("\n"));
        socket.close();
        return input;
    }
}
