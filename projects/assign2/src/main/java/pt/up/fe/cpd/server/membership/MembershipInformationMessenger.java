package pt.up.fe.cpd.server.membership;

import java.io.IOException;
import java.net.InetAddress;

import pt.up.fe.cpd.networking.TCPMessenger;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;

/*
Sends membership information over TCP
 */
public class MembershipInformationMessenger extends TCPMessenger {
    public MembershipInformationMessenger(InetAddress address, int port){
        super(address, port);
    }

    public void send(ClusterViewer clusterViewer) throws IOException {
        String message = "MEMBERSHIP\n\n" +
                clusterViewer.getNodeRepresentation() +
                '\n' +
                clusterViewer.getLogRepresentation();

        super.send(message.getBytes());
    }
}