package pt.up.fe.cpd.server.membership.tasks;

import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;
import pt.up.fe.cpd.server.membership.MembershipInformationMessenger;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MembershipInformationSender implements Runnable {
    final private ActiveNodeInfo nodeInfo;
    final private ClusterViewer clusterViewer;

    public MembershipInformationSender(ActiveNodeInfo nodeInfo, ClusterViewer clusterViewer){
        this.nodeInfo = nodeInfo;
        this.clusterViewer = clusterViewer;
    }

    @Override
    public void run(){
        int minWait = 50;
        int maxWait = 1000;
        int randomNum = ThreadLocalRandom.current().nextInt(minWait, maxWait);
        try {
            TimeUnit.MILLISECONDS.sleep(randomNum);
        } catch (InterruptedException e) {
            return;
        }
        System.out.println("["+ this.nodeInfo + "] Sending TCP membership info (waited " + randomNum + " ms)");
        MembershipInformationMessenger sender = new MembershipInformationMessenger(this.nodeInfo.getInetAddress(), this.nodeInfo.getPort() + 1);
        try {
            sender.send(clusterViewer);
        } catch (IOException e) {
            return;
        }

    }
}