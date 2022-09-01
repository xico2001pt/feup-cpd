package pt.up.fe.cpd.client;

import pt.up.fe.cpd.networking.FileTransfer;
import pt.up.fe.cpd.server.membership.MembershipService;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.text.ParseException;
import java.time.Instant;

import pt.up.fe.cpd.utils.HashUtils;
import pt.up.fe.cpd.utils.Pair;

// The test client should be invoked as follows: $ java TestClient <node_ap> <operation> [<opnd>]
public class TestClient {

    public static void main(String[] args) {
        if(args.length < 2){
            System.out.println("Invalid arguments");
            printUsage();
        }
        String node_ap =  args[0];
        String operation = args[1];

        try {
            switch(operation){
                case "join": join(node_ap); break;
                case "leave": leave(node_ap); break;
                case "get": get(node_ap, args[2]); break;
                case "put": put(node_ap, args[2]); break;
                case "delete": delete(node_ap, args[2]); break;
                case "view": view(node_ap); break;
            }
        } catch(IOException e){
            System.out.println("Couldn't connect to the store service, the node hasn't joined the cluster yet.");        
        }
    }

    public static void get(String node_ap, String key) throws IOException {
        Pair<InetAddress, Integer> parsedNodeAp;
        try {
            parsedNodeAp = parseNodeAp(node_ap);
        } catch(ParseException e){
            System.out.println("Invalid node_ap");
            printUsage();
            return;
        }
        InetAddress address = parsedNodeAp.first;
        int port = parsedNodeAp.second;
   
        Socket socket = new Socket(address, port);
        DataInputStream socketInputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream socketOutputStream = new DataOutputStream(socket.getOutputStream());

        // Send GET request to server
        socketOutputStream.write(("GET " + key + "\n").getBytes("UTF-8"));

        // Transfer file
        DataOutputStream fileOutputStream;
        try{
            fileOutputStream = new DataOutputStream(new FileOutputStream(key));
        } catch(FileNotFoundException e) {
            socketInputStream.close();
            socketOutputStream.close();
            socket.close();
            return;
        }

        boolean transferSuccessful = FileTransfer.transfer(socketInputStream, fileOutputStream);
        if(!transferSuccessful){
            File file = new File(key);
            file.delete();
        }

        fileOutputStream.close();
        socketInputStream.close();
        socketOutputStream.close();
        socket.close();
    }

    public static String put(String node_ap, String file_path) throws IOException {
        Pair<InetAddress, Integer> parsedNodeAp;
        try {
            parsedNodeAp = parseNodeAp(node_ap);
        } catch(ParseException e){
            System.out.println("Invalid node_ap");
            printUsage();
            return "";
        }
        InetAddress address = parsedNodeAp.first;
        int port = parsedNodeAp.second;
        
        byte[] key;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");    
            key = digest.digest((file_path + Instant.now().getEpochSecond()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { // This should never happen
            e.printStackTrace();
            return "";
        }

        Socket socket = new Socket(address, port);

        FileInputStream fileInputStream;
        try{
            fileInputStream = new FileInputStream(file_path);
        } catch(FileNotFoundException e){
            System.out.println("File " + file_path + " cannot be found.");
            socket.close();
            return "";
        }
        
        DataInputStream inputStream = new DataInputStream(fileInputStream);
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        outputStream.write(("PUT " + HashUtils.keyByteToString(key) + "\n\n").getBytes("UTF-8"));

        boolean transferSuccessful = FileTransfer.transfer(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        socket.close();

        String keyString = HashUtils.keyByteToString(key);
        System.out.println("File stored in key " + keyString);
        return keyString;
    }

    public static void delete(String node_ap, String key) throws IOException {
        Pair<InetAddress, Integer> parsedNodeAp;
        try {
            parsedNodeAp = parseNodeAp(node_ap);
        } catch(ParseException e){
            System.out.println("Invalid node_ap");
            printUsage();
            return;
        }
        InetAddress address = parsedNodeAp.first;
        int port = parsedNodeAp.second;
   
        Socket socket = new Socket(address, port);

        // Send GET request to server
        DataOutputStream socketOutputStream = new DataOutputStream(socket.getOutputStream());
        socketOutputStream.write(("DELETE " + key + "\n").getBytes("UTF-8"));
        socketOutputStream.close();
        socket.close();
    }

    public static void join(String node_ap){
        Pair<String, String> parsedAp;
        try {
            parsedAp = parseRMIAp(node_ap);
        } catch(ParseException e){
            System.out.println("Invalid node_ap");
            printUsage();
            return;
        }
        try {
            Registry registry = LocateRegistry.getRegistry(parsedAp.first);
            MembershipService stub = (MembershipService) registry.lookup(parsedAp.second);
            stub.join();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
    public static void leave(String node_ap){
        Pair<String, String> parsedAp;
        try {
            parsedAp = parseRMIAp(node_ap);
        } catch(ParseException e){
            System.out.println("Invalid node_ap");
            printUsage();
            return;
        }
        try {
            Registry registry = LocateRegistry.getRegistry(parsedAp.first);
            MembershipService stub = (MembershipService) registry.lookup(parsedAp.second);
            stub.leave();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void view(String node_ap){
        Pair<String, String> parsedAp;
        try {
            parsedAp = parseRMIAp(node_ap);
        } catch(ParseException e){
            System.out.println("Invalid node_ap");
            printUsage();
            return;
        }
        
        try {
            Registry registry = LocateRegistry.getRegistry(parsedAp.first);
            MembershipService stub = (MembershipService) registry.lookup(parsedAp.second);
            System.out.println(stub.view());
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
    private static Pair<InetAddress, Integer> parseNodeAp(String node_ap) throws IOException, ParseException {
        // Host: ip_addr:port
        String[] splitHost = node_ap.split(":");
        if(splitHost.length != 2){
            throw new ParseException("",0);
        }
        String addressString = splitHost[0];
        int port = Integer.parseInt(splitHost[1]);
        InetAddress address = InetAddress.getByName(addressString);
        return new Pair<>(address, port);
    }

    private static Pair<String, String> parseRMIAp(String ap) throws ParseException {
        // RMI: ip_addr:ip_addr_rmi
        String[] splitAp = ap.split(":");
        if(splitAp.length != 2){
            throw new ParseException("",0);
        }
        String rmiAddress = splitAp[0];
        String rmiObject = splitAp[1];
        return new Pair<String,String>(rmiAddress, rmiObject);
    }

    private static void printUsage(){
        System.out.println("Usage: java TestClient <node_ap> <operation> [<opnd>]");
        System.out.println("Example: java TestClient 127.0.0.1:127.0.0.1_9002 put ./file.txt");
        System.out.println("Example: java TestClient 127.0.0.1:9002 join");
    }
}