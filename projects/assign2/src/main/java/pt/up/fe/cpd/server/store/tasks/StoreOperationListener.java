package pt.up.fe.cpd.server.store.tasks;

import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.net.*;
import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.membership.ConnectionStatus;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;
import pt.up.fe.cpd.server.store.KeyValueStore;

public class StoreOperationListener implements Runnable {
    private final KeyValueStore keyValueStore;
    private final TCPListener listener;
    private final ExecutorService executor;
    private final ClusterSearcher searcher;
    private final ClusterViewer clusterViewer;
    private final InetAddress multicaAddress;
    private final int multicastPort;

    public StoreOperationListener(KeyValueStore keyValueStore, TCPListener listener, ExecutorService executor, ClusterSearcher searcher, ClusterViewer clusterViewer,
                                  InetAddress multicastAddress, int multicastPort){
        this.keyValueStore = keyValueStore;
        this.executor = executor;
        this.listener = listener;
        this.searcher = searcher;
        this.clusterViewer = clusterViewer;
        this.multicaAddress = multicastAddress;
        this.multicastPort = multicastPort;
    }

    @Override
    public void run() {
        while(clusterViewer.getConnectionStatus() == ConnectionStatus.CONNECTED) {
            try {
                Socket socket = listener.accept();
                executor.execute(new StoreOperationHandler(keyValueStore, socket, searcher, clusterViewer, executor, multicaAddress, multicastPort));
            } catch(IOException e) {}
        }
    }
}
