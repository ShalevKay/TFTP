package bgu.spl.net.impl.tftp;

import bgu.spl.net.impl.tftp.packets.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Keyboard implements Runnable {

    private final Socket sock;
    private final TftpProtocol protocol;
    private final Scanner in;

    public Keyboard(Socket sock, TftpProtocol protocol) {
        this.sock = sock;
        this.protocol = protocol;
        this.in = new Scanner(System.in);
    }

    @Override
    public void run() {

        try (Socket socket = this.sock;
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            while (!protocol.shouldTerminate()) {

                // Get the command from the user
                String command = in.nextLine();
                if (command != null && !command.isEmpty()) {

                    Packet packet = getPacket(command);
                    if (packet != null) {

                        // Update the last command packet in the protocol
                        // This is the start of a new packet-chain between
                        // the client's listening thread and the server
                        protocol.updateLastCommandPacket(packet);

                        // Switch back to the original version of those packets
                        // This is, so we can send the original packet to the server
                        if (packet instanceof PacketRRQ)
                            packet = new PacketWRQ(((PacketRRQ) packet).getFileName());
                        else if (packet instanceof PacketWRQ)
                            packet = new PacketRRQ(((PacketWRQ) packet).getFileName());

                        if (packet instanceof PacketERROR) {
                            // If the packet is an error packet, print the error message
                            System.out.println(((PacketERROR) packet).getErrorMsg());
                        }
                        else {

                            // Send the packet to the server
                            byte[] request = packet.toBytes();
                            out.write(request);
                            out.flush();

                            // Wait for packet-chain to end.
                            try { synchronized(protocol.getLock()) { protocol.getLock().wait(); } }
                            catch(InterruptedException ignored) {}
                        }
                    }
                    // If the command is invalid, print an error message
                    else { System.out.println(new PacketERROR((short) 4).getErrorMsg()); }
                }
            }
        }
        catch (IOException ignored) { }
    }

    // Returns the packet that corresponds to the given command
    // If the command is invalid, returns null
    // If the command is valid but the packet is invalid, returns a packet with an error message
    // If the command is valid and the packet is valid, returns the packet
    // RRQ/WRQ packets are opened in the opposite mode (RRQ opens a WRQ and vice versa)
    public Packet getPacket(String command) {

        // Split the command into parts
        // The first part is the operation
        String[] commandParts = command.split(" ");
        String operation = commandParts[0];

        String fileName;
        Packet error;
        switch (operation) {
            case "LOGRQ":
                if (commandParts.length != 2) return null;

                String username = command.substring(operation.length() + 1);
                return new PacketLOGRQ(username);
            case "DELRQ":
                if (commandParts.length != 2) return null;

                fileName = command.substring(operation.length() + 1);
                return new PacketDELRQ(fileName);
            case "RRQ":
                if (commandParts.length != 2) return null;

                fileName = command.substring(operation.length() + 1);
                PacketWRQ oppositeRRQ = new PacketWRQ(fileName); // Create a WRQ packet with the same file name

                error = oppositeRRQ.openFile(); // Create the file in the client's directory
                if (error instanceof PacketERROR) return error;
                else return oppositeRRQ;
            case "WRQ":
                if (commandParts.length != 2) return null;

                fileName = command.substring(operation.length() + 1);
                PacketRRQ oppositeWRQ = new PacketRRQ(fileName); // Create an RRQ packet with the same file name

                error = oppositeWRQ.openFile(); // Open the file in the client's directory
                if (error instanceof PacketERROR) return error;
                else return oppositeWRQ;
            case "DIRQ":
                if (commandParts.length != 1) return null;

                return new PacketDIRQ();
            case "DISC":
                if (commandParts.length != 1) return null;

                return new PacketDISC();
            default:
                return null;
        }
    }

}
