package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.impl.tftp.packets.*;
import bgu.spl.net.srv.Connections;

import java.util.Arrays;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private Connections<byte[]> connections;

    private boolean shouldTerminate;

    private PacketWRQ lastWRQPacket;
    private PacketRRQ lastRRQPacket;
    private PacketDIRQ lastDIRQPacket;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        this.shouldTerminate = false;
        this.lastWRQPacket = null;
        this.lastRRQPacket = null;
        this.lastDIRQPacket = null;
    }

    @Override
    public void process(byte[] message) {
        System.out.println("Received: " + Arrays.toString(message));

        Packet receivePacket = Packet.bytesToPacket(message);
        Packet resultPacket = null;

        short opcode = receivePacket.getOpcode();
        if (!connections.isLoggedIn(connectionId)) {
            if (opcode == 7) processLOGRQ(receivePacket);
            else if (opcode == 10) processDISC();
            else connections.send(connectionId, new PacketERROR((short) 6).toBytes());
        }
        else {
            switch (opcode) {
                case 1:
                    processRRQ(receivePacket);
                    break;
                case 2:
                    processWRQ(receivePacket);
                    break;
                case 3:
                    processDATA(receivePacket);
                    break;
                case 4:
                    processACK(receivePacket);
                    break;
                case 6:
                    processDIRQ(receivePacket);
                    break;
                case 7:
                    resultPacket = new PacketERROR((short) 7);
                    break;
                case 8:
                    processDELRQ(receivePacket);
                    break;
                case 10:
                    processDISC();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid opcode");
            }
        }

        // Send the result packet to the client
        if (resultPacket != null)
            connections.send(connectionId, resultPacket.toBytes());
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private void processRRQ(Packet receivedPacket) {
        lastRRQPacket = (PacketRRQ) receivedPacket;

        if (connections.isFileInUse(lastRRQPacket.getFileName())) {
            connections.send(connectionId, new PacketERROR((short)2).toBytes());
            return;
        }
        connections.setFileInUse(lastRRQPacket.getFileName(), true);

        Packet result = lastRRQPacket.openFile();
        if (result instanceof PacketACK) {
            byte[] data = lastRRQPacket.readData();
            if (data != null) result = new PacketDATA((short) 1, data);
            else {
                connections.setFileInUse(lastRRQPacket.getFileName(), false);
                lastRRQPacket = null;
                System.out.println("Closed file");
            }
        } else {
            // Error Received - Could not open file.
            connections.setFileInUse(lastRRQPacket.getFileName(), false);
        }
        connections.send(connectionId, result.toBytes());
    }

    // If WRQ packets are received, open the file for writing
    // and store the packet for future DATA packets
    private void processWRQ(Packet receivedPacket){
        lastWRQPacket = (PacketWRQ) receivedPacket;
        if (connections.isFileInUse(lastWRQPacket.getFileName())) {
            connections.send(connectionId, new PacketERROR((short)2).toBytes());
            return;
        }
        connections.setFileInUse(lastWRQPacket.getFileName(), true);

        Packet error = lastWRQPacket.openFile();
        if (error instanceof PacketERROR)
            connections.setFileInUse(lastWRQPacket.getFileName(), false);
        connections.send(connectionId, error.toBytes());
    }

    // If DATA packets are received after a WRQ packet
    // handle aggregating the data and writing it to the file
    private void processDATA(Packet receivedPacket){
        if (lastWRQPacket != null) {
            short blockNumber = ((PacketDATA) receivedPacket).getBlockNumber();
            byte[] data = ((PacketDATA) receivedPacket).getData();
            Packet error = lastWRQPacket.writeData(data);
            if (error != null) connections.send(connectionId,error.toBytes());
            else if (data.length < 512) {
                // If the data is less than 512 bytes, close the file and send a BCAST packet
                lastWRQPacket.closeFile();
                if (data.length > 0) {
                    connections.send(connectionId, new PacketACK(blockNumber).toBytes());
                }
                connections.sendAll(new PacketBCAST(lastWRQPacket.getFileName(), true).toBytes());
                connections.setFileInUse(lastWRQPacket.getFileName(), false);
                lastWRQPacket = null;
            } else connections.send(connectionId, new PacketACK(blockNumber).toBytes());
        }
    }

    private void processACK(Packet receivedPacket){
        short blockNumber = ((PacketACK)receivedPacket).getBlockNumber();
        if (blockNumber <= 0) connections.send(connectionId, new PacketERROR((short) 0, "Invalid block number").toBytes()) ;
        if (lastRRQPacket != null) {
            byte[] data = lastRRQPacket.readData();
            if (data != null) {
                if (data.length < 512) {
                    // Transferred successfully.
                    lastRRQPacket.closeFile();
                    connections.setFileInUse(lastRRQPacket.getFileName(), false);
                    lastRRQPacket = null;
                }
                connections.send(connectionId, new PacketDATA((short) (1 + blockNumber), data).toBytes());
            }
            else {
                // Error has occurred while reading the file.
                lastRRQPacket.closeFile();
                connections.setFileInUse(lastRRQPacket.getFileName(), false);
                
                lastRRQPacket = null;
                connections.send(connectionId, new PacketERROR((short) 0, "Failed to read data from file").toBytes());
            }
        }
        else if (lastDIRQPacket != null) {
            byte[] nextBytes = lastDIRQPacket.getNextFileBytes();
            if (nextBytes.length == 0) {
                lastDIRQPacket = null;
            }
            else {
                connections.send(connectionId, new PacketDATA((short) (blockNumber + 1), nextBytes).toBytes());
            }
        }
        else {
            connections.send(connectionId, new PacketERROR((short) 0, "No RRQ/DIRQ packet received").toBytes());
        }
    }

    private void processDIRQ(Packet receivedPacket){
        lastDIRQPacket = (PacketDIRQ) receivedPacket;
        lastDIRQPacket.acquireFiles();
        byte[] nextBytes = lastDIRQPacket.getNextFileBytes();
        if (nextBytes.length == 0) lastDIRQPacket = null;
        else connections.send(connectionId, new PacketDATA((short) 1, nextBytes).toBytes());
    }

    private void processLOGRQ(Packet receivedPacket){
        String username = ((PacketLOGRQ) receivedPacket).getUsername();
        if(connections.login(connectionId, username))
            connections.send(connectionId, new PacketACK((short) 0).toBytes());
        else connections.send(connectionId, new PacketERROR((short) 7).toBytes());
    }

    private void processDELRQ(Packet receivedPacket){
        PacketDELRQ delrq = (PacketDELRQ) receivedPacket;
        if (connections.isFileInUse(delrq.getFileName())) {
            connections.send(connectionId, new PacketERROR((short)2).toBytes());
            return;
        }
        connections.setFileInUse(delrq.getFileName(), true);

        if (delrq.deleteFile()) {
            connections.send(connectionId, new PacketACK((short) 0).toBytes());
            connections.sendAll(new PacketBCAST(delrq.getFileName(), false).toBytes());
        }
        else connections.send(connectionId, new PacketERROR((short) 1).toBytes());

        connections.setFileInUse(delrq.getFileName(), false);
    }

    // If a DISC packet is received, terminate the connection
    private void processDISC(){
        if (connections.logout(connectionId)){
            shouldTerminate = true;
            connections.send(connectionId, new PacketACK((short) 0).toBytes());
            connections.removeConnection(connectionId);
        } else {
            connections.send(connectionId, new PacketERROR((short) 0, "Failed to logout").toBytes());
        }
    }
    
}
