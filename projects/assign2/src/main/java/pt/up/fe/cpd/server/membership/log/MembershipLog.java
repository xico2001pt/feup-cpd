package pt.up.fe.cpd.server.membership.log;

import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.cpd.server.NodeInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

public class MembershipLog {
    private LinkedList<MembershipLogEntry> entries;

    public MembershipLog(){
        this.entries = new LinkedList<>();
    }

    public MembershipLog(File file){
        this.entries = new LinkedList<>();
        this.readFromFile(file);
    }

    public List<MembershipLogEntry> getEntries() {
        return entries;
    }

    public boolean addEntry(MembershipLogEntry entry) {
        /*
        Merging Rules
        - Rule 1: If the event is already in the local log, skip it
        - Rule 2: If the event is older (you should use the membership count) than the event for that member in the local log, skip it
        - Rule 3: If the event is new, i.e. there is no event in the local log for that member, add that event to the tail of the log
            (assuming that events at the tail are the most recent)
        - Rule 4: If the event is newer than the event for that member in the local log, remove the event for that member from the local
            log, and add the newer event at the tail of the log (i.e. as if it was a new event)
        */
        
        if (entries.contains(entry)) return false;
        else {
            for (MembershipLogEntry compareEntry : entries) {
                if (entry.getAddress().equals(compareEntry.getAddress()) &&
                    entry.getPort() == compareEntry.getPort()) {
                    if (entry.getMembershipCounter() < compareEntry.getMembershipCounter()) return false;
                    if (entry.getMembershipCounter() > compareEntry.getMembershipCounter()) {
                        this.entries.remove(compareEntry);
                        this.entries.add(entry);
                        return true;
                    }
                }
            }
        }

        if (this.entries.size() >= 32) {
            this.entries.removeFirst();
        }
        this.entries.add(entry);
        return true;
    }

    public int getCounter(NodeInfo node){
        for (MembershipLogEntry compareEntry : entries) {
            if (node.getAddress().equals(compareEntry.getAddress()) &&
                node.getPort() == compareEntry.getPort()) {
                return compareEntry.getMembershipCounter();
            }
        }
        return 0;
    }

    public void writeToFile(File file) {
        try{
            FileOutputStream outputStream = new FileOutputStream(file);
            String string = entries.stream()
                                .map(e -> e.toString())
                                .collect(Collectors.joining("\n")) 
                                + "\n";
            outputStream.write(string.getBytes("UTF-8"));
            outputStream.close();
        } catch(FileNotFoundException e){
            System.out.println("File not found");
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void readFromFile(File file){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line;
            while((line = reader.readLine()) != null){
                String[] splitLine = line.split(" ");
                String[] nodeId = splitLine[0].split(":");
                String address = nodeId[0];
                int port = Integer.parseInt(nodeId[1]);
                int membershipCounter = Integer.parseInt(splitLine[1]);
                this.addEntry(new MembershipLogEntry(address, port, membershipCounter));
            }

            reader.close();
        } catch(FileNotFoundException e){

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return entries.stream().map(e -> e.toString()).collect(Collectors.joining(", "));
    }
}
