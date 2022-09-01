package pt.up.fe.cpd.server.membership.log;

public class MembershipLogEntry {
    private String address;
    private int port;
    int membershipCounter;

    public MembershipLogEntry(String address, int port, int membershipCounter){
        this.address = address;
        this.port = port;
        this.membershipCounter = membershipCounter;
    }

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public int getMembershipCounter() {
        return this.membershipCounter;
    }

    @Override
    public String toString(){
        return address + ":" + port + " " + this.membershipCounter;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MembershipLogEntry)) {return false;}
        MembershipLogEntry entry = (MembershipLogEntry) obj;
        return this.address.equals(entry.getAddress()) && this.port == entry.getPort() && this.membershipCounter == entry.getMembershipCounter();
    }
}
