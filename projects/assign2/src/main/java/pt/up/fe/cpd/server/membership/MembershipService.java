package pt.up.fe.cpd.server.membership;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MembershipService extends Remote {
    void join() throws RemoteException;

    void leave() throws RemoteException;

    String view() throws RemoteException;
}
