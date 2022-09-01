package pt.up.fe.cpd.server.membership;

public class Connection {
    ConnectionStatus status;
    
    public Connection(){
        this.status = ConnectionStatus.DISCONNECTED;
    }

    public Connection(ConnectionStatus status){
        this.status = status;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }
}
