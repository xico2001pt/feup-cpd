:: Start registry
pushd .\build\classes\java\main
start rmiregistry
popd

:: Start stores
start java -cp .\build\classes\java\main -Djava.rmi.server.codebase=file:.\build\classes\java\main\ pt.up.fe.cpd.server.Store 224.0.0.1 9001 127.0.0.1 9002
start java -cp .\build\classes\java\main -Djava.rmi.server.codebase=file:.\build\classes\java\main\ pt.up.fe.cpd.server.Store 224.0.0.1 9001 127.0.0.2 9002
start java -cp .\build\classes\java\main -Djava.rmi.server.codebase=file:.\build\classes\java\main\ pt.up.fe.cpd.server.Store 224.0.0.1 9001 127.0.0.3 9002

:: Start client(s)
pause
java -classpath .\build\classes\java\main pt.up.fe.cpd.client.TestClient 127.0.0.1 join
pause
java -classpath .\build\classes\java\main pt.up.fe.cpd.client.TestClient 127.0.0.1:9002 put whatever

::java -classpath .\build\classes\java\main pt.up.fe.cpd.client.TestClient 127.0.0.2 join
::pause
::java -classpath .\build\classes\java\main pt.up.fe.cpd.client.TestClient 127.0.0.3 join