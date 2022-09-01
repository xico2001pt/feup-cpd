package pt.up.fe.cpd.server.membership;

import pt.up.fe.cpd.networking.MulticastMessenger;
import java.io.IOException;
import java.net.*;

public class MembershipMessenger extends MulticastMessenger {
    MembershipEvent event;
    int membershipCounter;

    public MembershipMessenger(MembershipEvent event, int membershipCounter, InetAddress address, int port){
        super(address, port);
        this.event = event;
        this.membershipCounter = membershipCounter;
    }

    public void send(String storeAddress, int storePort) throws IOException {
        String header = storeAddress + " " + storePort + " " +  membershipCounter;
        this.send(header, "");
    }

    public void send(String header, String body) throws IOException {
        StringBuilder message = new StringBuilder();
        message.append(event);
        
        if(!header.isEmpty()){
            message.append(" ").append(header);
        }
        
        if(!body.isEmpty()){
            message.append("\n\n").append(body);
        }

        byte[] data = message.toString().getBytes();
        super.send(data);
    }
}
