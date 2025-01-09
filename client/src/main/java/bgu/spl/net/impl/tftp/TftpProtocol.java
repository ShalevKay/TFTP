package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.impl.tftp.packets.*;

import java.nio.charset.StandardCharsets;

public class TftpProtocol implements MessagingProtocol<byte[]> {

    private Packet lastCommandPacket = null;
    private boolean shouldTerminate = false;
    private final Object lock = new Object();

    @Override
    public byte[] process(byte[] msg) {
        Packet receivePacket = Packet.bytesToPacket(msg);
        short opcode = receivePacket.getOpcode();

        Packet resultPacket = null;
        switch(opcode) {
            case 3:
                resultPacket = processDATA((PacketDATA) receivePacket);
                break;
            case 4:
                resultPacket = processACK((PacketACK) receivePacket);
                break;
            case 5:
                resultPacket = processError((PacketERROR) receivePacket);
                break;
            case 9:
                resultPacket = processBCAST((PacketBCAST) receivePacket);
                break;
            default:
                break;
        }

        if (resultPacket != null) { return resultPacket.toBytes(); }
        return null;
    }


    public Packet processDATA(PacketDATA packet) {

        // Check the previous packet the keyboard thread sent
        if (lastCommandPacket != null) {

            // If the last command requested RRQ
            if (lastCommandPacket instanceof PacketWRQ) {

                // Client requested RRQ and server sent DATA
                // we saved the last command packet as RRQ, so we can write data to file
                Packet error = ((PacketWRQ) lastCommandPacket).writeData(packet.getData());

                if (error != null) { synchronized(lock) { lock.notifyAll(); } }
                else if (packet.getData().length < 512) {

                    // End of file
                    System.out.println("RRQ " + ((PacketWRQ) lastCommandPacket).getFileName() + " complete");
                    ((PacketWRQ) lastCommandPacket).closeFile();

                    // We don't need to do anything with the DATA
                    lastCommandPacket = null;

                    // Notify the waiting keyboard thread
                    synchronized(lock) { lock.notifyAll(); }
                }
                // Send ACK
                else return new PacketACK(packet.getBlockNumber());

            } else if (lastCommandPacket instanceof PacketDIRQ) {

                // Client requested DIRQ and server sent DATA
                // we saved the last command packet as DIRQ, so we can read data from directory
                byte[] nextData = packet.getData();
                ((PacketDIRQ) lastCommandPacket).addBytes(nextData);

                if (nextData.length < 512) {

                    // End of directory
                    byte[] allBytes = ((PacketDIRQ) lastCommandPacket).getAllBytes();
                    System.out.println(new String(allBytes, StandardCharsets.UTF_8));

                    // We don't need to do anything with the DATA
                    lastCommandPacket = null;

                    // Notify the waiting keyboard thread
                    synchronized(lock) { lock.notifyAll(); }
                }
                // Send ACK
                return new PacketACK(packet.getBlockNumber());
            }
        }

        // Return null, so we won't send another packet
        return null;
    }

    public Packet processACK(PacketACK packet) {

        // Print ACK message
        System.out.println("ACK " + packet.getBlockNumber());

        // Check the previous packet the keyboard thread sent
        if (lastCommandPacket != null) {

            // If the last command requested WRQ
            if (lastCommandPacket instanceof PacketRRQ) {

                // Client requested WRQ and server sent ACK
                // we saved the last command packet as RRQ, so we can read data from file
                byte[] nextData = ((PacketRRQ) lastCommandPacket).readData();

                // If the file is empty, we can close it
                if (nextData == null || nextData.length == 0)  {

                    // End of file
                    System.out.println("WRQ " + ((PacketRRQ) lastCommandPacket).getFileName() + " complete");
                    ((PacketRRQ) lastCommandPacket).closeFile();

                    // We don't need to do anything with the ACK
                    lastCommandPacket = null;

                    // Notify the waiting keyboard thread
                    synchronized(lock) { lock.notifyAll(); }
                }

                // If the file is not empty, we can send the next data packet
                if (nextData == null)
                    nextData = new byte[0];
                return new PacketDATA((short) (packet.getBlockNumber() + 1), nextData);

            } else if (lastCommandPacket instanceof PacketDISC) {

                // Client requested DISC and server sent ACK
                // we saved the last command packet as DISC, so we can terminate the connection
                shouldTerminate = true;

                // We don't need to do anything with the ACK
                lastCommandPacket = null;

                // Notify the waiting keyboard thread
                synchronized(lock) { lock.notifyAll(); }

                // Exit the program
                System.exit(0);

            } else if (lastCommandPacket instanceof PacketLOGRQ || lastCommandPacket instanceof PacketDELRQ) {

                // Rest of the possible commands from the keyboard
                // We don't need to do anything with the ACK
                lastCommandPacket = null;

                // Notify the waiting keyboard thread
                synchronized(lock) { lock.notifyAll(); }
            }
        }

        // Return null, so we won't send another packet
        return null;
    }

    public Packet processError(PacketERROR packet) {
        if (lastCommandPacket != null) {

            // Print error message
            System.out.println(packet.getErrorMsg());

            // If the last command packet was WRQ, delete the file
            // This is, so the client will not have a corrupted file
            if (lastCommandPacket instanceof PacketWRQ)
                if (!((PacketWRQ) lastCommandPacket).delete())
                    System.out.println("Failed to delete file: " + ((PacketWRQ) lastCommandPacket).getFileName());

            // Notify the waiting keyboard thread
            synchronized(lock) { lock.notifyAll(); }
        }

        // Return null, so we won't send another packet
        return null;
    }

    public Packet processBCAST(PacketBCAST packet) {
        // Print BCAST message
        System.out.println("BCAST " + packet.addedOrDeleted() + " " + packet.getFileName());

        // No need to notify the waiting keyboard thread
        // since other packets are going to wake it up
        // Return null, so we won't send another packet
        return null;
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public void updateLastCommandPacket(Packet packet) {
        this.lastCommandPacket = packet;
    }

    public Object getLock() { return lock; }
}
