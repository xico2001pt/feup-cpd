pkill -f pt.up.fe.cpd.server.Store
pkill rmiregistry
sleep 1
./start_registry.sh
sleep 1
./start_stores.sh