# Assign 2

## Usage:

To simplify the use of our project we created some helper scripts: `start_registry.sh`, `start_stores.sh`, `store.sh`, `client.sh` and `cleanup.sh`

All the following commands should be run in the root of the project (cpd/assign2 in the git repo).


The project should be compiled by running `gradle build`.

The RMI registry should be initialized using:
```
sh start_registry.sh
```

To start an individual Store the command 
```
sh store.sh <IP_mcast_addr> <IP_mcast_port> <node_id>  <Store_port>
[example] sh store.sh 224.0.0.1 9001 127.0.0.1 9002
```

To quickly start multiple servers we also created the `start_stores.sh` which creates 6 servers using ip 127.0.0.1 through 127.0.0.6.

The client can be used in the following manner:
```
sh client.sh 127.0.0.1:127.0.0.1_9002 join
sh client.sh 127.0.0.1:127.0.0.1_9002 leave
sh client.sh 127.0.0.1:127.0.0.1_9002 view
sh client.sh 127.0.0.1:9002 get {key}
sh client.sh 127.0.0.1:9002 put {file}
sh client.sh 127.0.0.1:9002 delete {key}
```

In the first 3 options the <node_ap> contains the RMIRegister address followed by the Store RMI Object that, by default, is bound to the name address_port.

The `cleanup.sh` script will terminate all active store processes and the rmi registry.

## Revisions

- Fixed race condition on MulticastListener
